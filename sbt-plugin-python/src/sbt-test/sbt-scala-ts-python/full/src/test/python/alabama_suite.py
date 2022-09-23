import unittest

from generated import alabama
from generated.alabama import Alabama

class AlabamaSuite(unittest.TestCase):

    def test(self):
        al: Alabama = 'AL'
        self.assertEqual(al, alabama.AlabamaInvariants.entryName)
        self.assertEqual(al, alabama.AlabamaInhabitant)
        
