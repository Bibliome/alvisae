#!/bin/env python

import alvisae
import os.path


USER_FILE = 'users.csv'
TASK_FILE = 'tasks.csv'


class ImportAnnotations(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'import annotations', '--import-annotations')
        self.add_argument('--json', metavar='DIR', dest='json_dir', required=True, help='Path containing annotation JSON files')
        self.add_argument('--campaign', metavar='CID', dest='campaign_id', required=True, help='Campaign identifier (AlvisAE internal numeric)')
        self.add_argument('--user', metavar='USER', dest='user', required=True, help='User name')
        self.add_argument('--task', metavar='TASK', dest='task', required=False, default=None, help='Task name')

    def run(self):
        args = self.parse_args()
        if args.task is None:
            if not os.path.exists(TASK_FILE):
                raise RuntimeError('either --task or an existing ' + TASK_FILE + ' file is mandatory')
        else:
            with open(TASK_FILE, 'w') as f:
                f.write('0\t%s\n' % args.task)
        with open(USER_FILE, 'w') as f:
            f.write('0\t%s\n' % args.user)
        end_cli = [
            '-d',
            args.json_dir,
            '--campaignId',
            args.campaign_id,
            '--userList',
            USER_FILE,
            '--taskList',
            TASK_FILE
        ]
        self.launch(args, end_cli)


if __name__ == '__main__':
    ImportAnnotations().run()
