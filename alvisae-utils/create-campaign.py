#!/bin/env python

import alvisae


class CreateCampaign(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a campaign', '--create-campaign')
        self.add_argument('--name', metavar='NAME', dest='campaign_name', required=True, help='Campaign name')
        self.add_argument('--schema', metavar='SCHEMA', dest='schema', default='./schema.json', help='Path to annotation schema JSON file')
        self.add_argument('--workflow', metavar='WORKFLOW', dest='workflow', default='./workflow.xml', help='Path to annotation workflow XML file')

    def run(self):
        args = self.parse_args()
        end_cli = [
            '-c',
            args.campaign_name,
            '-s',
            args.schema,
            '-w',
            args.workflow
        ]
        self.launch(args, end_cli)


if __name__ == '__main__':
    CreateCampaign().run()
