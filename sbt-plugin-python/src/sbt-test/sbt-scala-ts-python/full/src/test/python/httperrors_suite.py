import unittest

import typing

from generated.httperrors import HttpErrorsInvariants


class HttpErrorsSuite(unittest.TestCase):

    def test(self):
        self.assertEqual(HttpErrorsInvariants.NotFound, '404')

        self.assertEqual(HttpErrorsInvariants.InternalServerError, '500')
        
        
