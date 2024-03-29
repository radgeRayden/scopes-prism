using import radl.strfmt slice String
let symbols = (import .scopes-std-symbols.symbols)

import UTF-8
using import itertools

fn escape-pattern (str)
    ->> str UTF-8.decoder
        retain
            inline (c)
                switch c
                pass char"["
                pass char"]"
                pass char"\\"
                pass char"/"
                pass char"^"
                pass char"$"
                pass char"."
                pass char"|"
                pass char"?"
                pass char"*"
                pass char"+"
                pass char"("
                pass char")"
                pass char"{"
                pass char"}"
                do
                    UTF-8.char32 "\\"
                default
                    -1
            map
                inline (code-point)
                    code-point
        flatten
        filter
            (x) -> (x != -1)
        UTF-8.encoder
        string.collector 256

inline gen-symbol-match (kind)
    let result =
        fold (result = str"") for k v in (getattr symbols kind)
            .. (escape-pattern (v as string)) "|" result
    let result = (lslice result ((countof result) - 1))
    .. "/(^|[,'()\\[\\]{} ])(?:" result ")(?=$|[,'()\\[\\]{} ])/gm"

let name argc argv = (script-launch-args)
let export-name =
    if ((argc > 0) and (('from-rawstring String (argv @ 0)) == "-nodemode"))
        S"module.exports"
    else
        S"Prism.languages.scopes"

vvv io-write!
f""""${export-name} = {
         'comment': [
             {
                 pattern: /^(?<pad> *)#.*(?:\n|$)(?: (?=\k<pad>).+(?:\n|$))*/gm
             },
             {
                 pattern: /#.*?(?=(\n|$))/
             }
         ],
         'string': [
             {
                 pattern: /(?<pad>(?: )*)"""".*(?:\n|$)(?:(?:(?:\k<pad>    .*| *))(\n|$))*/gm,
                 greedy: true
             },
             {
                 pattern: /"(?:\\"|(?!["\n]).)*"/,
                 greedy: true,
                 inside: {
                     'escape-code': {
                         pattern: /\\x[0-9A-Fa-f]{2}|\\./,
                         alias: "variable"
                     }
                 }
             }
         ],
         'quoted-symbol': {
             pattern: /'[^,'()\[\]{}\s\n]+/,
             alias: ["constant", "bold"]
         },
         'keyword': {
             pattern: ${gen-symbol-match 'keywords},
             lookbehind: true
         },
         'sugar': {
             pattern: ${gen-symbol-match 'sugar-macros},
             lookbehind: true,
             alias: "keyword"
          },
         'spice': {
             pattern: ${gen-symbol-match 'spice-macros},
             lookbehind: true,
             alias: ["function", "bold"]
          },
         'function': {
             pattern: ${gen-symbol-match 'functions},
             lookbehind: true
         },
         'operator': {
             pattern: ${gen-symbol-match 'operators},
             lookbehind: true
         },
         'constant': {
             pattern: ${gen-symbol-match 'special-constants},
             lookbehind: true
         },
         'type': {
             pattern: ${gen-symbol-match 'types},
             lookbehind: true,
             alias: "important"
         },
         'global-symbol': {
             pattern: ${gen-symbol-match 'global-symbols},
             lookbehind: true,
             alias: "variable"
         },
         'number': {
             pattern: /(^|[,'()\[\]{} ])[+-]?(?:\d+(?:\.\d+)?|0b[01]+(?:\.[01]+)?|0o[0-7]+(?:\.[0-7]+)?|0x[\da-fA-F]+(?:\.[\da-fA-F]+)?)(?:e[+-]?\d+)?(?:\:(?:[ui](?:8|16|32|64)|usize|f32|f64))?(?=$|[,'()\[\]{} ])/gm,
             lookbehind: true
         }
     };
