import unittest

from datetime import datetime

from generated.busline import BusLine
from generated.name import Name
from generated.foo import Foo


class FooSuite(unittest.TestCase):

    def test(self):
        now = datetime.now()

        busline = BusLine(
            id=1,
            name='Line #1',
            stopIds=['A', 'B', 'C'])

        foo1: Foo = Foo(
            id=1,
            namesp=[2,'trois'],
            row=['row1', busline, now],
            score=10,
            rates={})

        # --

        foo2: Foo = Foo(
            id=2,
            namesp=[3,'quatre'],
            row=['row2', busline, now],
            score=100,
            rates={
                'low': 0.1,
                'high': 10.2,
            })

        self.assertNotEqual(foo1, foo2)
