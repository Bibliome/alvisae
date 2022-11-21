#!/bin/env python

import alvisae
import os
import os.path
import shutil


TMP_DIR = '.tmp/add-documents'


class AddDocuments(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'register documents', '--add-documents')
        self.add_argument('--json', metavar='DIR', dest='json_dir', required=True, help='Path containing document JSON files')
        self.add_argument('--one-by-one', dest='one_by_one', action='store_true', default=False, help='Register documents one by one, resilient to duplicate documents')

    def run(self):
        args = self.parse_args()
        if args.one_by_one:
            end_cli = ['-d', TMP_DIR]
            for jfile in self._get_json_files(args):
                os.makedirs(TMP_DIR, exist_ok=False)
                shutil.copy(jfile, TMP_DIR)
                self.launch(args, end_cli)
                shutil.rmtree(TMP_DIR)
        else:
            end_cli = ['-d', args.json_dir]
            self.launch(args, end_cli)

    def _get_json_files(self, args):
        for root, _, filenames in os.walk(args.json_dir):
            for fn in filenames:
                if fn.endswith('.json'):
                    yield os.path.join(root, fn)


if __name__ == '__main__':
    AddDocuments().run()
