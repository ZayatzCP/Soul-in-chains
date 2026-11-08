#!/usr/bin/env python3
"""Imports Blockbench "Java Block/Item" models into the mod, replacing the placeholder art.

Usage (from the repository root):
    python3 tools/import_models.py <export_folder> [--dry-run] [--assets <assets_dir>]

Put the exported .json models and their .png textures into one folder (sub-folders are fine).
File names decide what is replaced:

  inventory / hand model         worn on the player (see MODELS.md for the coordinates)
  ---------------------------    -----------------------------------------------------------
  wreath_of_manipulation.json    wreath_of_manipulation_worn.json            (head)
  focusing_boots.json            focusing_boots_worn.json                    (both legs)
                                 or focusing_boots_worn_right/_left.json
  focusing_gloves.json           -  (held casting tool, no worn model)
  ring_of_reversal.json          ring_of_reversal_worn.json                  (right arm)
  mantle_of_reflection.json      mantle_of_reflection_worn.json              (body)
  rose_gold_ingot.json

Textures are copied to textures/item/models/ and every reference is rewritten to
arcane_artifacts:item/models/<name>, so they are stitched into the item atlas. The script
validates what Minecraft would otherwise reject or silently break (element bounds, rotation
angles, missing textures) and stops without writing anything if there is an error.
"""
import argparse
import json
import os
import shutil
import sys

NS = 'arcane_artifacts'
ICONS = ['wreath_of_manipulation', 'focusing_boots', 'focusing_gloves', 'ring_of_reversal',
         'mantle_of_reflection', 'rose_gold_ingot', 'old_parchment', 'researcher_notebook']
# export name -> list of (target worn model, mirror?)
WORN = {
    'wreath_of_manipulation_worn': [('wreath_of_manipulation', False)],
    'focusing_boots_worn': [('focusing_boots_right', False), ('focusing_boots_left', False)],
    'focusing_boots_worn_right': [('focusing_boots_right', False)],
    'focusing_boots_worn_left': [('focusing_boots_left', False)],
    'ring_of_reversal_worn': [('ring_of_reversal', False)],
    'mantle_of_reflection_worn': [('mantle_of_reflection', False)],
}
VALID_ANGLES = {-45.0, -22.5, 0.0, 22.5, 45.0}
DEFAULT_ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', NS)


class Problems:
    def __init__(self):
        self.errors, self.warnings = [], []

    def error(self, msg):
        self.errors.append(msg)

    def warn(self, msg):
        self.warnings.append(msg)


def find_files(folder):
    found = {}
    for root, _, files in os.walk(folder):
        for f in files:
            found.setdefault(f, os.path.join(root, f))
    return found


def texture_basename(ref):
    ref = ref.split(':', 1)[-1]
    return ref.rsplit('/', 1)[-1]


def validate_elements(name, model, problems):
    for i, el in enumerate(model.get('elements', [])):
        for key in ('from', 'to'):
            for v in el.get(key, []):
                if v < -16 or v > 32:
                    problems.error(f'{name}: element #{i} "{key}" {el[key]} is outside -16..32 (Minecraft refuses the model)')
        rot = el.get('rotation')
        if rot and float(rot.get('angle', 0)) not in VALID_ANGLES:
            problems.error(f'{name}: element #{i} rotation {rot.get("angle")} - Java models only allow -45, -22.5, 0, 22.5, 45')
        for face_name, face in el.get('faces', {}).items():
            if not str(face.get('texture', '')).startswith('#'):
                problems.error(f'{name}: element #{i} face {face_name} has no texture variable')


def mirror_x(model):
    """Mirrors a worn model across x = 8 (right arm/leg -> left arm/leg)."""
    m = json.loads(json.dumps(model))
    swap = {'east': 'west', 'west': 'east'}
    for el in m.get('elements', []):
        fx, tx = el['from'][0], el['to'][0]
        el['from'][0], el['to'][0] = 16 - tx, 16 - fx
        faces = {}
        for fname, face in el.get('faces', {}).items():
            face = dict(face)
            if 'uv' in face and fname not in ('east', 'west'):
                u1, v1, u2, v2 = face['uv']
                face['uv'] = [u2, v1, u1, v2]
            faces[swap.get(fname, fname)] = face
        el['faces'] = faces
        rot = el.get('rotation')
        if rot:
            rot['origin'][0] = 16 - rot['origin'][0]
            if rot.get('axis') in ('y', 'z'):
                rot['angle'] = -float(rot['angle'])
    return m


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('folder')
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--assets', default=DEFAULT_ASSETS)
    args = ap.parse_args()

    files = find_files(args.folder)
    problems = Problems()
    textures_to_copy = {}   # basename -> source path
    outputs = {}            # target path -> model dict

    jobs = []
    for name in ICONS:
        if name + '.json' in files:
            jobs.append((name, files[name + '.json'], [(os.path.join('models', 'item', name + '.json'), False)]))
    for export, targets in WORN.items():
        if export + '.json' in files:
            jobs.append((export, files[export + '.json'],
                         [(os.path.join('models', 'item', 'worn', t + '.json'), mirror) for t, mirror in targets]))

    known = {n + '.json' for n in ICONS} | {n + '.json' for n in WORN}
    for f in files:
        if f.endswith('.json') and f not in known:
            problems.warn(f'{f}: unknown file name, ignored (see the table at the top of this script)')
    if not jobs:
        problems.error('no recognised model files found')

    for label, path, targets in jobs:
        try:
            with open(path, encoding='utf-8') as fh:
                model = json.load(fh)
        except (OSError, ValueError) as e:
            problems.error(f'{label}: cannot read JSON ({e})')
            continue
        if 'elements' not in model and 'parent' not in model:
            problems.error(f'{label}: neither "elements" nor "parent" - is this a Java Block/Item export?')
            continue
        validate_elements(label, model, problems)
        new_textures = {}
        for key, ref in model.get('textures', {}).items():
            if ref.startswith('#'):
                new_textures[key] = ref
                continue
            if ref.startswith('minecraft:') or (ref.startswith(NS + ':') and '/models/' not in ref and texture_basename(ref) + '.png' not in files):
                new_textures[key] = ref   # vanilla or already-existing mod texture
                continue
            base = texture_basename(ref)
            src = files.get(base + '.png')
            if src is None:
                problems.error(f'{label}: texture "{ref}" not found (expected {base}.png in the export folder)')
                continue
            previous = textures_to_copy.get(base)
            if previous and open(previous, 'rb').read() != open(src, 'rb').read():
                problems.error(f'{label}: two different textures are both named {base}.png')
            textures_to_copy[base] = src
            new_textures[key] = f'{NS}:item/models/{base}'
        model['textures'] = new_textures
        if 'particle' not in new_textures and new_textures:
            model['textures']['particle'] = next(iter(new_textures.values()))
        if label.startswith('wreath_of_manipulation'):
            tints = {f.get('tintindex') for el in model.get('elements', []) for f in el.get('faces', {}).values()}
            if not tints & {1, 2, 3, 4} and 'layer1' not in model.get('textures', {}):
                problems.warn(f'{label}: no faces with tintindex 1-4, so the adaptation colours will not show on the wreath')
        if not label.endswith('_worn') and '_worn_' not in label and 'mirrored' not in label and 'elements' in model \
                and 'gui' not in model.get('display', {}):
            problems.warn(f'{label}: no "gui" display transform - set one in Blockbench\'s Display tab so the inventory icon is framed well')
        for target, mirror in targets:
            outputs[target] = mirror_x(model) if mirror else model

    for w in problems.warnings:
        print('WARNING:', w)
    for e in problems.errors:
        print('ERROR:  ', e)
    if problems.errors:
        print('Nothing was written.')
        return 1

    for base, src in sorted(textures_to_copy.items()):
        dst = os.path.join(args.assets, 'textures', 'item', 'models', base + '.png')
        print(('would copy ' if args.dry_run else 'copy  ') + os.path.relpath(src) + ' -> ' + os.path.relpath(dst))
        if not args.dry_run:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(src, dst)
            meta = src + '.mcmeta'
            if os.path.exists(meta):
                shutil.copyfile(meta, dst + '.mcmeta')
    for target, model in sorted(outputs.items()):
        dst = os.path.join(args.assets, target)
        print(('would write ' if args.dry_run else 'write ') + os.path.relpath(dst))
        if not args.dry_run:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, 'w', encoding='utf-8') as fh:
                json.dump(model, fh, indent=2)
                fh.write('\n')
    print('Done.' if not args.dry_run else 'Dry run finished, nothing written.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
