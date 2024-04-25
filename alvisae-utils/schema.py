from abc import abstractmethod
from collections.abc import Iterator, Sequence


class Schema:
    _annotation_types: dict[str, 'AnnotationType'] = {}

    @staticmethod
    def get_type(name: str) -> 'AnnotationType':
        return Schema._annotation_types[name]

    @staticmethod
    def kind_types(kind: int) -> Iterator['AnnotationType']:
        for adef in Schema._annotation_types.values():
            if adef.kind == kind:
                yield adef

    @staticmethod
    def _add_type(atype: 'AnnotationType') -> None:
        assert atype.name not in Schema._annotation_types
        Schema._annotation_types[atype.name] = atype

    @staticmethod
    def to_json() -> dict:
        return {k: v.to_json() for (k, v) in Schema._annotation_types.items()}

    @staticmethod
    def from_json(j: dict) -> None:
        for aj in j.values():
            AnnotationType.from_json(aj)

    @staticmethod
    def _kind_name(kind: int) -> str:
        match kind:
            case 0:
                return 'Text-bound'
            case 1:
                return 'Group'
            case 2:
                return 'Relation'

    @staticmethod
    def console() -> None:
        for kind in range(3):
            print('\u001b[1m%s\u001b[0m' % Schema._kind_name(kind))
            for adef in Schema.kind_types(kind):
                adef.console('  ')


class AnnotationType:
    def __init__(self, kind: int, spec_def_key: str, name: str, color: str):
        assert color.startswith('#') and len(color) == 7
        self._kind = kind
        self._spec_def_key = spec_def_key
        self._name = name
        self._color = color
        self._properties: dict[str, Property] = {}
        Schema._add_type(self)

    @property
    def kind(self) -> int:
        return self._kind

    @property
    def name(self) -> str:
        return self._name

    @property
    def color(self) -> str:
        return self._color

    def get_property(self, name: str) -> 'Property':
        return self._properties[name]

    def add_properties(self, *props: 'Property'):
        for prop in props:
            assert prop.key not in self._properties
            self._properties[prop.key] = prop
        return self

    def to_json(self) -> dict:
        j = dict(
            type=self._name,
            kind=self._kind,
            color=self._color,
            propDef={p.key: p.to_json() for p in self._properties.values()}
        )
        j[self._spec_def_key] = self._spec_def_to_json()
        return j

    @abstractmethod
    def _spec_def_to_json(self) -> dict:
        pass

    @staticmethod
    def from_json(j: dict) -> None:
        name = j['type']
        color = j['color']
        kind = j['kind']
        match kind:
            case 0:
                adef = TextBound._from_json(name, color, j['txtBindingDef'])
            case 1:
                adef = Group._from_json(name, color, j['groupDef'])
            case 2:
                adef = Relation._from_json(name, color, j['relationDef'])
            case _:
                raise RuntimeError()
        props = [Property.from_json(jp) for jp in j['propDef'].values()]
        adef.add_properties(*props)

    def _console_name(self) -> str:
        hex_color = self.color.lstrip('#')
        rgb_color = tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))
        term_color = ';'.join(str(c) for c in rgb_color)
        if self._kind == 0:
            return '\u001b[30m\u001b[48;2;%sm%s\u001b[0m' % (term_color, self.name)
        return '\u001b[48;2;255;255;255m\u001b[38;2;%sm%s\u001b[0m' % (term_color, self.name)

    def console(self, indent: str = '  ') -> None:
        print('%s%s' % (indent, self._console_name()))
        self._console('  ')
        print('    Properties')
        for p in self._properties.values():
            p.console('      ')

    @abstractmethod
    def _console(self, indent: str) -> None:
        pass


class TextBound(AnnotationType):
    def __init__(self, name: str, color: str, minFrag: int = 1, maxFrag: int = 10):
        AnnotationType.__init__(self, 0, 'txtBindingDef', name, color)
        assert minFrag <= maxFrag
        self._crossingAllowed = True
        self._boundRef = ''
        self._minFrag = minFrag
        self._maxFrag = maxFrag

    def _spec_def_to_json(self) -> dict:
        return dict(
            crossingAllowed = self._crossingAllowed,
            boundRef = self._boundRef,
            minFrag = self._minFrag,
            maxFrag = self._maxFrag
        )

    @staticmethod
    def _from_json(name: str, color: str, j: dict) -> 'TextBound':
        return TextBound(name, color, j['minFrag'], j['maxFrag'])

    def _console(self, indent: str = '  ') -> None:
        pass


class Group(AnnotationType):
    def __init__(self, name: str, color: str, homogeneous: bool, compTypes: Sequence[AnnotationType], minComp: int = 1, maxComp: int = 9999999):
        AnnotationType.__init__(self, 1, 'groupDef', name, color)
        assert minComp <= maxComp
        self._homogeneous = homogeneous
        self._compTypes = compTypes
        self._minComp = minComp
        self._maxComp = maxComp

    def _spec_def_to_json(self) -> dict:
        return dict(
            minComp = self._minComp,
            maxComp = self._maxComp,
            compType = [a.name for a in self._compTypes],
            homogeneous = self._homogeneous
        )

    @staticmethod
    def _from_json(name: str, color: str, j: dict) -> 'Group':
        compType = [Schema.get_type(ct) for ct in j['compType']]
        return Group(name, color, j['homogeneous'], compType, j['minComp'], j['maxComp'])

    def _console(self, indent: str = '  ') -> None:
        homogeneity = 'homogeneous' if self._homogeneous else 'heterogeneous'
        print('  %s%s: %s' % (indent, homogeneity, ' | '.join(ct.name for ct in self._compTypes)))


class Relation(AnnotationType):
    def __init__(self, name: str, color: str, args: dict[str, Sequence[AnnotationType]]):
        AnnotationType.__init__(self, 2, 'relationDef', name, color)
        self._args = args

    def _spec_def_to_json(self) -> list:
        return [dict(((k, [a.name for a in v]),)) for (k, v) in self._args.items()]

    @staticmethod
    def _from_json(name: str, color: str, j: dict) -> 'Relation':
        args0 = [list(a.items())[0] for a in j]
        args = {k: [Schema.get_type(t) for t in v] for (k, v) in args0}
        return Relation(name, color, args)

    def _console(self, indent: str = '  ') -> None:
        for k, types in self._args.items():
            print('  %s%s: %s' % (indent, k, ', '.join(t.name for t in types)))


class Property:
    def __init__(self, key: str, mandatory: bool, minVal: int = 1, maxVal: int = 10):
        assert minVal <= maxVal
        self._key = key
        self._mandatory = mandatory
        self._minVal = minVal
        self._maxVal = maxVal

    @property
    def key(self) -> str:
        return self._key

    def to_json(self) -> dict:
        return dict(
            key = self._key,
            minVal = self._minVal,
            maxVal = self._maxVal,
            mandatory = self._mandatory,
            valType = self._val_type_to_json()
        )

    @abstractmethod
    def _val_type_to_json(self) -> dict:
        pass

    @staticmethod
    def from_json(j: dict) -> 'Property':
        key, mandatory, minVal, maxVal, valType = [j[k] for k in ('key', 'mandatory', 'minVal', 'maxVal', 'valType')]
        if 'valTypeName' not in valType:
            return FreeProperty(key, mandatory, minVal, maxVal)
        if valType['valTypeName'] == 'ClosedDomain':
            return ClosedSetProperty(key, mandatory, valType['domain'], valType['defaultVal'], minVal, maxVal)
        if valType['valTypeName'] == 'TyDI_conceptRef':
            return TyDIProperty(key, mandatory, valType['TyDIRefBaseURL'], minVal, maxVal)
        return FreeProperty(key, mandatory, minVal, maxVal)

    def console(self, indent: str = '      ') -> None:
        print('%s%s (%s)' % (indent, self._key, self._console_type()))

    @abstractmethod
    def _console_type(self) -> str:
        pass


class FreeProperty(Property):
    def __init__(self, key: str, mandatory: bool, minVal: int = 1, maxVal: int = 10):
        Property.__init__(self, key, mandatory, minVal, maxVal)

    def _val_type_to_json(self) -> dict:
        return dict(
            closedDomain = False,
            defaultVal = ''
        )

    def _console_type(self) -> str:
        return '*'


class ClosedSetProperty(Property):
    def __init__(self, key: str, mandatory: bool, domain: Sequence[str], default: str, minVal: int = 1, maxVal: int = 10):
        Property.__init__(self, key, mandatory, minVal, maxVal)
        assert default in domain
        self._domain = domain
        self._default = default

    def _val_type_to_json(self) -> dict:
        return dict(
            valTypeName = 'ClosedDomain',
            defaultVal = self._default,
            domain = self._domain
        )

    def _console_type(self) -> str:
        return ' | '.join(self._domain)


class TyDIProperty(Property):
    def __init__(self, key: str, mandatory: bool, base_url: str, minVal: int = 1, maxVal: int = 10):
        Property.__init__(self, key, mandatory, minVal, maxVal)
        assert base_url.startswith('http://') or base_url.startswith('https://')
        self._base_url = base_url

    def _val_type_to_json(self) -> dict:
        return dict(
            valTypeName = 'TyDI_conceptRef',
            TyDIRefBaseURL = self._base_url
        )

    def _console_type(self) -> str:
        return self._base_url


if __name__ == '__main__':
    import sys
    import json
    if len(sys.argv) == 1:
        js = json.load(sys.stdin)
    else:
        with open(sys.argv[1]) as f:
            js = json.load(f)
    Schema.from_json(js)
    Schema.console()
