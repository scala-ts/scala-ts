import unittest

from generated import hi
from generated.hi import Hi


class HiSuite(unittest.TestCase):

    def test(self):
        v: Hi = 'Hi'
        self.assertEqual(v, hi.HiInhabitant)
        
