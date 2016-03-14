(ns jekyll-clojure
  (:require [clj-yaml.core :as yaml]
            [me.raynes.fs :as fs]
            [selmer.parser :as selmer]))



(def site-template-dir "site_template/")
(def includes-dir "_includes/")
(def layouts-dir "_layouts/")
(def posts-dir "_posts/")
(def sass-dir "_sass/")
(def css-dir "css/")
(def index-file "index.html")

(def output-dir "_site")

(defn find-yaml [f]
  (str (first (re-find #"(?m)(---\s*\n.*?\n?)((---|\.\.\.)\s*$\n?)" (slurp f)))))

; hacky solution that just replaces the YAML with nothing
(defn find-content [f]
  (str (clojure.string/replace (slurp f) #"(?m)(---\s*\n.*?\n?)((---|\.\.\.)\s*$\n?)" "")))

(defrecord layout [site name path ext data content])

(defn read-layouts []
  (def files (fs/find-files (fs/file fs/*cwd* site-template-dir layouts-dir) #".*\.html"))
  (for [f files]
    (if (fs/file? f)
      (map->layout {:name (fs/name f)
                    :path f
                    :ext (fs/extension f)
                    :data (yaml/parse-string (clojure.string/replace (find-yaml f) #"---" ""))
                    :content (find-content f)}))))

(doseq [l (read-layouts)]
  (println (:data l)))
