# Word Level Autocomplete with a Frequency Trie

I learned about [Tries](https://github.com/tinfante/algorithms-and-data-structures/tree/master/Data-Structures/Trie) recently. Here is a small experiment that extends the idea to be sensitive to character frequencies, and applies it to real data. Specifically to Spanish Google Ngrams unigram frequencies (re-used from [frecuencia-palabras-letras](https://github.com/tinfante/frecuencias-palabras-letras)).


Start the repl,
```bash
$ lein repl
```
A character is associated to a map that has a `:count` key (recording that character's frequency of appearance) and
may have `:next` and `:end` keys. If the character is the last character in a word, it has `:end` `true`. If another
character follows the current character to form a longer word, then `next`'s value is a similar map for the next character.

For example, 
```clojure
word-autocomplete.core=> (def trie (insert-many-word-freqs '(["on" 3] ["only" 5] ["or" 7] ["one" 4] ["ore" 2])))
#'word-autocomplete.core/trie

word-autocomplete.core=> (pprint trie)
{"o"
 {:count 21,
  :next
  {"n"
   {:count 12,
    :end true,
    :next
    {"l" {:count 5, :next {"y" {:count 5, :end true}}},
     "e" {:count 4, :end true}}},
   "r" {:count 9, :end true, :next {"e" {:count 2, :end true}}}}}}
nil
```
Note that *on* has a count of 12 instead of 3, since the frequency of *on* as a substring of *only* (5) and *one* (4) is also recorded. The idea behind this is to first offer the shortest word for a shared prefix.
```clojure
word-autocomplete.core=> (suggest trie "o")
([12 "on"] [9 "or"] [5 "only"] [4 "one"] [2 "ore"])

```
Now lets try with the first 100,000 unigram types ordered by frequency from Google Ngrams for Spanish, which represent 68 billion tokens.

```clojure
word-autocomplete.core=> (def trie (insert-many-word-freqs (read-tsv "resources/google-1gram-spanish-freq.tsv" 100000)))
#'word-autocomplete.core/trie

word-autocomplete.core=> (time (take 10 (suggest trie "escor")))
"Elapsed time: 0.237248 msecs"
([594564N "escoria"] [407969N "escorial"] [79503N "escorias"] [55973N "escorbuto"] [50264N "escorzo"]
 [47878N "escorpión"] [40298N "escorrentía"] [27666N "escorpiones"] [15868N "escorzos"])

word-autocomplete.core=> (time (take 10 (suggest trie "escorp")))
"Elapsed time: 0.262306 msecs"
([47878N "escorpión"] [27666N "escorpiones"])
```
🦂🦂🦂


## TODO
* Have a function read one character at a time, and call suggest function.
* Improve `insert-many-word-freqs`, produces stack overflow when given the first 1,000,000 types (almost half the data). Bear in mind though, that at that point the data is rather noisy and has lots of spelling mistakes, includes words in other languages, etc, so this is strictly a programming challange. Shouldn't Clojure using loop/recur (tail optimization) not have stack issues? See [this](https://stuartsierra.com/2015/04/26/clojure-donts-concat) and [this](https://medium.com/@nikosfertakis/clojure-lazy-evaluation-and-stack-overflow-exceptions-1b8ee732ba0b).
* Make the code fully functional if possible. Try using immutable data instead of an atom.
* Remove code duplication.
