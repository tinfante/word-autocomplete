(ns word-autocomplete.core
  (:gen-class)
  (:require [clojure.string :as s])
  )


(defn insert-one-word
  "Inserts a single word in a trie (a map)."
  [trie word]
  (let [split-word (map str word)]
    (loop [rest-chars split-word
           updated-trie trie
           node-path []]
      (if (not (empty? rest-chars))
        (let [nodes (conj node-path (first rest-chars))]
          (recur (rest rest-chars)
                 (update-in updated-trie
                            nodes
                            #(merge-with + % {:count 1}))
                 (conj nodes :next)))
        (update-in updated-trie (drop-last 1 node-path) merge {:end true})))))


(defn insert-many-words
  "Inserts many words (vector of words) into a trie (usually an empty map)."
  [trie words]
  (reduce #(insert-one-word %1 %2) trie words)
  )


(defn has-word
  "Returns true if word in trie, nil otherwise."
  [trie word]
  (let [word-split (map str word)
        intrm-keys (conj (vec (take (dec (count word)) (repeat :next))) :end)]
    (get-in trie (interleave word-split intrm-keys))))


(defn has-prefix
  "Returns the prefix node, or nil if prefix not found in trie."
  [trie prefix]
  (get-in trie (interleave (map str prefix) (repeat :next))))


(defn -descend-tree
  "Given any node, walks down the tree to all word endings. it accumulates
  the possible suffixes and the :count attribute of the last character in
  an atom (accum). This function is meant to be used inside suffix-suggestions
  function."
  [node prefix accum]
  (if (not (nil? node))
    (loop [child-chars (keys node)]
      (if (not (empty? child-chars))
        (do
          (if (get-in node [(first child-chars) :end])
            (swap! accum
                   (fn
                     [v]
                     (conj @accum [(get-in node [(first child-chars) :count])
                                   (str prefix (first child-chars))
                                   ])))
            )
          (if (get-in node [(first child-chars) :next])
            (-descend-tree (get-in node [(first child-chars) :next])
                           (str prefix (first child-chars))
                           accum
                           ))
          (recur (rest child-chars)))
        ))))


(defn suffix-suggestions
  [prefix-node]
  (let
    [accum (atom [])]
    (do (-descend-tree prefix-node "" accum) (reverse (sort @accum)))
    )
  )


(defn insert-word-freq
  "Almost exactly the same as insert-one-word func, but also accepts a word
  frequency. It would be nice to avoid code duplication... but I'm also lazy,
  so TODO."
  [trie word freq]
  (let [split-word (map str word)]
    (loop [rest-chars split-word
           updated-trie trie
           node-path []]
      (if (not (empty? rest-chars))
        (let [nodes (conj node-path (first rest-chars))]
          (recur (rest rest-chars)
                 (update-in updated-trie
                            nodes
                            #(merge-with + % {:count freq}))
                 (conj nodes :next)))
        (update-in updated-trie (drop-last 1 node-path) merge {:end true})))))


(defn insert-many-word-freqs
  "Inserts many word-frequency pairs (a list of two item vectors) into an empty
  trie (a map)."
  [word-freqs]
  (loop [trie {}
         wf word-freqs]
    (if (empty? wf)
      trie
      (let [[wrd frq] (first wf)]
        (recur (insert-word-freq trie wrd frq) (rest wf))
        ))))


(defn read-tsv
  [filename]
  (map (fn [line] (let [[word freq] (s/split line #"\t")]
                    [word (bigint freq)]))
       (s/split (slurp filename) #"\n")))


(defn suggest
  [trie prefix]
  (let [suff-freqs (suffix-suggestions (has-prefix trie prefix))]
    (map (fn [x] [(first x) (str prefix (second x))]) suff-freqs)
    )
  )


(defn -main
  []
  (println "Use with 'lein repl'.")
  )
