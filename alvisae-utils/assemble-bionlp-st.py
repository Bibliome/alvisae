#!/bin/env python


import argparse
import shutil
import os
import glob


PARSER = argparse.ArgumentParser('assemble files to .txt, .a1 and .a2 files')
PARSER.add_argument('--source', metavar='DIR', required=True, dest='source', help='directory where to find source files')
PARSER.add_argument('--target', metavar='DIR', required=True, dest='target', help='directory where to write .txt, .a1 and .a2 files')
PARSER.add_argument('--ner', action='store_true', required=False, dest='ner', default=False, help='NER task')
PARSER.add_argument('--norm', action='store_true', required=False, dest='norm', default=False, help='normalization task')
PARSER.add_argument('--re', action='store_true', required=False, dest='re', default=False, help='RE task')


args = PARSER.parse_args()
if not (args.ner or args.norm or args.re):
    raise RuntimeError('select at least one task')


os.makedirs(args.target, exist_ok=True)
DOCIDS = []
for txt in glob.glob(os.path.join(args.source, 'txt/*.txt')):
    docid, _ = os.path.splitext(os.path.basename(txt))
    DOCIDS.append(docid)
    shutil.copy(txt, args.target)


def copy(source, target):
    with open(os.path.join(args.source, source)) as f:
        for line in f:
            target.write(line)


for docid in DOCIDS:
    with open(os.path.join(args.target, f'{docid}.a1'), 'w') as a1:
        with open(os.path.join(args.target, f'{docid}.a2'), 'w') as a2:
            copy(f'layout/{docid}.lay', a1)
            copy(f'entities/{docid}.ent', a2 if args.ner else a1)
            if args.norm:
                copy(f'normalizations/{docid}.norm', a2)
            if args.re:
                copy(f'relations/{docid}.rel', a2)
