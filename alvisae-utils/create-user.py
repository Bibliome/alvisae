#!/bin/env python

import alvisae


class CreateUser(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a user', '--create-user')
        self.add_argument('--login', metavar='NAME', dest='login', required=True, help='User name')
        self.add_argument('--password', metavar='PASSWORD', dest='password', required=True, help='User password')

    def run(self):
        args = self.parse_args()
        end_cli = [
            '--userName',
            args.login,
            '--password',
            args.password
        ]
        self.launch(args, end_cli)


if __name__ == '__main__':
    CreateUser().run()
