import unittest

import typing

from generated.constants import ConstantsInvariants
from generated.name import Name


class ConstantsSuite(unittest.TestCase):

    def test(self):
        self.assertEqual(ConstantsInvariants.code, 1)

        unknownName: Name = ConstantsInvariants.UnknownName
        self.assertEqual(unknownName, Name('unknown'))

        defaultName: Name = ConstantsInvariants.defaultName
        self.assertEqual(defaultName, Name('default'))

        ls: typing.List[int] = ConstantsInvariants.list
        self.assertEqual(ls, [1, 2])

        d: typing.Dict[str, typing.List[Name]] = ConstantsInvariants.dict

        self.assertEqual(d, {
            "specific": [unknownName, defaultName, Name("*")],
            "invalid": [Name("failed")]
        })

        excluded: typing.List[int] = ConstantsInvariants.excluded
        self.assertEqual(excluded, ['foo', 'bar'])

        filtered: typing.List[int] = ConstantsInvariants.filtered
        self.assertEqual(filtered, ['foo', 'bar', 'filtered'])

        ns: typing.List[Name] = ConstantsInvariants.names
        self.assertEqual(ns, [unknownName, defaultName, Name("test")])

        
        
