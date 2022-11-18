#!/bin/env python

import alvisae
import json
import sys


class CreateCampaign(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a campaign', '--create-campaign')
        self.add_argument('--name', metavar='NAME', dest='campaign_name', required=True, help='Campaign name')
        self.add_argument('--schema', metavar='SCHEMA', dest='schema', default='./schema.json', help='Path to annotation schema JSON file')
        self.add_argument('--workflow', metavar='WORKFLOW', dest='workflow', default='./workflow.xml', help='Path to annotation workflow XML file')
        self.add_argument('--store-id', metavar='FILE', dest='store_id', required=False, help='Path to file where to write the created campaign id')
        self.add_argument('--multiple', metavar='FILE', dest='multiple', required=False, help='Create multiple campaigns with the same schema and workflow, read strings from specified file (--name and --store_id must be patterns with %%s)')

    def run(self):
        self.args = self.parse_args()
        with open(self.args.schema) as f:
            schema = json.load(f)
        if 'schema' not in schema:
            schema = {'schema': schema}
        schema_file = '.schema.json'
        with open(schema_file, 'w') as f:
            json.dump(schema, f, indent=4)
        if self.args.multiple is None:
            end_cli = [
                '-c',
                self.args.campaign_name,
                '-s',
                schema_file,
                '-w',
                self.args.workflow
            ]
            self.launch(self.args, end_cli)
        else:
            with open(self.args.multiple) as f:
                for line in f:
                    self.args.string = line.strip()
                    if self.args.string == '':
                        continue
                    end_cli = [
                        '-c',
                        self.args.campaign_name % self.args.string,
                        '-s',
                        schema_file,
                        '-w',
                        self.args.workflow
                    ]
                    self.launch(self.args, end_cli)

    def handle_stdout(self, stdout):
        if self.args.store_id is None:
            sys.stdout.write(stdout)
        else:
            cid, *_ = stdout.split('\t')
            if self.args.multiple is None:
                store_id = self.args.store_id
            else:
                store_id = self.args.store_id % self.args.string
            with open(store_id, 'w') as f:
                f.write(cid)
            sys.stderr.write('campaign identifier written in ' + store_id + '\n')


if __name__ == '__main__':
    CreateCampaign().run()
