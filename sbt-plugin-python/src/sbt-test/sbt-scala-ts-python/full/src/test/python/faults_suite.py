import unittest

import typing

from generated.faults import FaultsInvariants


class FaultsSuite(unittest.TestCase):

    def test(self):
        self.assertEqual(FaultsInvariants.http, ['404', '500'])
        
        
