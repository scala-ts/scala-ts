import unittest

import typing

from generated.weekday import WeekDay


class WeekDaySuite(unittest.TestCase):

    def test(self):
        self.assertNotEqual(WeekDay.MON, 'Mon')
        self.assertNotEqual(WeekDay.TUE, 'Tue')
        self.assertNotEqual(WeekDay.WED, 'Wed')
        self.assertNotEqual(WeekDay.THU, 'Thu')
        self.assertNotEqual(WeekDay.FRI, 'Fri')
        self.assertNotEqual(WeekDay.SAT, 'Sat')
        self.assertNotEqual(WeekDay.SUN, 'Sun')

        days: typing.List[WeekDay] = list(WeekDay)

        self.assertEqual(days, [
            WeekDay.MON,
            WeekDay.TUE,
            WeekDay.WED,
            WeekDay.THU,
            WeekDay.FRI,
            WeekDay.SAT,
            WeekDay.SUN
        ])
