#!/bin/env python

import json
import argparse


class DefWrapper:
    def __init__(self, odef):
        self.odef = odef

    @staticmethod
    def of(value):
        if isinstance(value, dict):
            return DefWrapper(value)
        if isinstance(value, list):
            return list(DefWrapper.of(v) for v in value)
        return value

    def values(self):
        for value in self.odef.values():
            yield DefWrapper.of(value)

    def items(self):
        for key, value in self.odef.items():
            yield key, DefWrapper.of(value)

    def __contains__(self, item):
        return item in self.odef

    def __getitem__(self, key):
        return DefWrapper.of(self.odef[key])

    def __getattr__(self, attr):
        try:
            return DefWrapper.of(self.odef[attr])
        except KeyError:
            raise AttributeError(attr + ' in ' + str(self.odef))

    def has_kind(self, kind):
        return self.kind == kind

    def is_text_bound(self):
        return self.has_kind(0)

    def is_group(self):
        return self.has_kind(1)

    def is_relation(self):
        return self.has_kind(2)

    def term_color(self):
        hex_color = self.color.lstrip('#')
        rgb_color = tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))
        return ';'.join(str(c) for c in rgb_color)

    @property
    def color_type(self):
        if self.is_text_bound:
            return '\u001b[30m\u001b[48;2;%sm%s\u001b[0m' % (self.term_color(), self.type)
        return '\u001b[48;2;255;255;255m\u001b[38;2;%sm%s\u001b[0m' % (self.term_color(), self.type)

    @property
    def value_type(self):
        val_type = self.valType
        if 'valTypeName' not in val_type:
            return '*'
        if val_type.valTypeName.startswith('TyDI_'):
            return self.valType.TyDIRefBaseURL
        if val_type.valTypeName == 'ClosedDomain':
            return ' | '.join(val_type.domain)
        return str(val_type)

    def args(self):
        for argDef in self.relationDef:
            yield from argDef.items()

    @property
    def homogeneity(self):
        if self.groupDef.homogeneous:
            return 'homogeneous'
        return 'heterogeneous'


def bold(s):
    return '\u001b[1m%s\u001b[0m' % s


class SchemaTool(argparse.ArgumentParser):
    def __init__(self):
        argparse.ArgumentParser.__init__(self, description='display AlvisAE schema')
        self.add_argument('schema_file', metavar='SCHEMA', help='AlvisAE schema file')

    def run(self):
        args = self.parse_args()
        schema = self._read_schema(args)
        print(bold('Text bounds'))
        for tb in self._iter_def(schema, 0):
            print('  %s' % tb.color_type)
            for pdef in tb.propDef.values():
                print('    %s (%s)' % (pdef.key, pdef.value_type))
        print(bold('Relations'))
        for rel in self._iter_def(schema, 2):
            print('  %s' % rel.color_type)
            for role, types in rel.args():
                print('    %s: %s' % (role, ' | '.join(types)))
            for pdef in rel.propDef.values():
                print('    %s (%s)' % (pdef.key, pdef.value_type))
        print(bold('Groups'))
        for grp in self._iter_def(schema, 1):
            print('  %s' % grp.color_type)
            print('    %s: %s' % ((grp.homogeneity, ' | '.join(grp.groupDef.compType))))
            for pdef in rel.propDef.values():
                print('    %s (%s)' % (pdef.key, pdef.value_type))

    def _read_schema(self, args):
        with open(args.schema_file) as f:
            schema = json.load(f)
        if 'schema' in schema:
            schema = schema['schema']
        return dict((k, DefWrapper(v)) for (k, v) in schema.items())

    def _iter_def(self, schema, kind):
        for adef in schema.values():
            if adef.kind == kind:
                yield adef


if __name__ == '__main__':
    SchemaTool().run()
