import unittest

from generated import barnum
from generated.barnum import BarNum


class BarNumSuite(unittest.TestCase):

    def test(self):
        v: BarNum = 'BarNum'
        self.assertEqual(v, barnum.BarNumInhabitant)
        
