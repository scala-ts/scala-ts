import unittest

import time
from datetime import datetime

from generated.busline import BusLine
from generated.name import Name
from generated.bar import Bar


class BarSuite(unittest.TestCase):

    def test(self):
        now = datetime.now()
        now_time = time.gmtime()

        bar1 = Bar(
            name=Name('One'),
            aliases=[],
            age=10,
            amount=None,
            transports=[],
            updated=now,
            created=now,
            time=now_time)

        self.assertEqual(bar1.name, 'One')
        self.assertEqual(len(bar1.aliases), 0)
        self.assertEqual(bar1.age, 10)
        self.assertIsNone(bar1.amount)
        self.assertEqual(len(bar1.transports), 0)
        self.assertEqual(bar1.updated, now)
        self.assertEqual(bar1.created, now)
        self.assertEqual(bar1.time, now_time)

        # --
        busline = BusLine(
            id=1,
            name='Line #1',
            stopIds=['A', 'B', 'C'])

        bar2 = Bar(
            name=Name('Two'),
            aliases=[Name('Deux'), Name('Ni')],
            age=20,
            amount=100,
            transports=[busline],
            updated=now,
            created=now,
            time=now_time)

        self.assertEqual(bar2.name, 'Two')
        self.assertEqual(bar2.aliases, ['Deux', 'Ni'])
        self.assertEqual(bar2.age, 20)
        self.assertEqual(bar2.amount, 100)
        self.assertEqual(bar2.transports, [busline])
        self.assertEqual(bar2.updated, now)
        self.assertEqual(bar2.created, now)
        self.assertEqual(bar2.time, now_time)

        self.assertNotEqual(bar1, bar2)
