import unittest

from generated import lorem
from generated.lorem import Lorem


class LoremSuite(unittest.TestCase):

    def test(self):
        v: Lorem = 'Lorem'
        self.assertEqual(v, lorem.LoremInhabitant)
        
