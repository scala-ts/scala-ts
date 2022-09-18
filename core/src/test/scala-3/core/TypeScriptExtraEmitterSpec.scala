package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet

private[core] trait TypeScriptExtraEmitterSpec { self: TypeScriptEmitterSpec =>
  import TypeScriptEmitterSpec.emit
  import TranspilerCompat.ns

  "Scala3 support" should {
    "emit enumeration with invariants" >> {
      "as entries" in {
        emit(ListSet(TranspilerExtraSpec.colorDecl)) must beTypedEqualTo(
          s"""const ${ns}ColorEntries = {
  Red: 'Red',
  Green: 'Green',
  Blue: 'Blue',
};

export type ${ns}Color = keyof (typeof ${ns}ColorEntries);

export const ${ns}Color = {
  ...${ns}ColorEntries,
  values: Object.keys(${ns}ColorEntries)
} as const;

class ${ns}ColorExtra {
  public purple: ReadonlyArray<${ns}Color> = [ ${ns}Color.Red, ${ns}Color.Blue ];
}

export ${ns}ColorInvariants = new ${ns}ColorExtra();

export function is${ns}Color(v: any): v is ${ns}Color {
  return ${ns}Color.values.includes(v);
}
"""
        )
      }

      "as enum" in {
        emit(
          ListSet(TranspilerExtraSpec.colorDecl),
          declMapper = DeclarationMapper.enumerationAsEnum
        ) must beTypedEqualTo(s"""export enum ${ns}Color {
  Red = 'Red',
  Green = 'Green',
  Blue = 'Blue'
}

export const ${ns}ColorValues: Array<${ns}Color> = [
  ${ns}Color.Red,
  ${ns}Color.Green,
  ${ns}Color.Blue
];

export function is${ns}Color(v: any): v is ${ns}Color {
  return (
    v == 'Red' ||
    v == 'Green' ||
    v == 'Blue'
  );
}

class ${ns}ColorExtra {
  public purple: ReadonlyArray<${ns}Color> = [ ${ns}Color.Red, ${ns}Color.Blue ];
}

export ${ns}ColorInvariants = new ${ns}ColorExtra();
""")
      }
    }
  }
}
