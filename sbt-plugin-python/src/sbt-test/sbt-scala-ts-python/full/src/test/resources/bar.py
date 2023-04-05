# Generated by ScalaTS 0.5.17-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass
import typing  # noqa: F401
import datetime  # noqa: F401
import time  # noqa: F401

from generated import name  # noqa: F401
from generated.name import Name
from generated import transport  # noqa: F401
from generated.transport import Transport


# Declare interface Bar
@dataclass
class Bar:
    name: Name
    aliases: typing.List[Name]
    age: int
    amount: typing.Optional[complex]
    transports: typing.List[Transport]
    updated: datetime.datetime
    created: datetime.datetime
    time: time.struct_time