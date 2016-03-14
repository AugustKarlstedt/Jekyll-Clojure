;;; Written by August Karlstedt

(ns jekyll-clojure
  (:require [clj-yaml.core :as yaml]
            [me.raynes.fs :as fs]
            [selmer.parser :as selmer]
            [markdown.core :as markdown]))

; some basic directories
(def site-template-dir "site_template/")
(def includes-dir "_includes/")
(def config-file "_config.yml")
(def index-file #"index\.html")

(def output-dir "_site")

; some tags aren't supported!
(selmer.validator/validate-off!)
(selmer.parser/set-resource-path! (str (fs/file site-template-dir)))

; parse the default config
(def payload (yaml/parse-string (slurp (fs/file site-template-dir config-file))))

; regex to find YAML
(defn find-yaml [f]
  (str (first (re-find #"(?m)(---[\w\W\n]*---)" f))))

; hacky solution that just replaces the YAML with nothing
(defn find-content [f]
  (str (clojure.string/replace f #"(?m)(---[\w\W\n]*---)" "")))

; our base "class"
(defrecord document [name path base ext relative-path data content output])

; read files
(defn read-files [files-dir file-regex]
  (def files (fs/find-files files-dir file-regex))
  (zipmap (for [f files]
            (fs/name f))
          (for [f files]
            (let [file-contents (slurp f)]
            (map->document {:name (fs/name f)
                            :path f
                            :base (fs/base-name f)
                            :ext (fs/extension f)
                            :data (yaml/parse-string (clojure.string/replace (find-yaml file-contents) #"---" ""))
                            :content (find-content file-contents)})))))

; set up some defaults
(def layouts (read-files (fs/file site-template-dir) #".*\.html"))
(def posts (read-files (fs/file site-template-dir) #".*\.markdown"))
(def index (read-files (fs/file site-template-dir) index-file))

(println (str "Found " (count layouts) " layouts."))
(println (str "Found " (count posts) " posts."))
(println (str "Found " (count index) " index."))

; helper functions
(defn get-yaml-data [d]
  (:data d))

(defn get-content [d]
  (:content d))

(defn get-path [d]
  (:path d))

(defn get-base-name [d]
  (:base d))

(defn get-layout-type [d]
  (:layout (get-yaml-data d)))

(defn get-layout [d]
  (layouts d))

(defn render-from-markdown [d]
  (markdown/md-to-html-string (:content d)))

(defn render-from-liquid [d p]
  (selmer/render-file (:base d) (str (fs/file site-template-dir output-dir (:name d)) ".html") p))

;d = document
;p = payload
(defn render [d p]
  (def layout (get-layout (get-layout-type d)))
  (def merged-payload (merge p (get-yaml-data d) {:content (render-from-markdown d)}))
  (if (nil? layout)
      (render-from-liquid d merged-payload)
      (render layout merged-payload)))

;write
(defn write-file [d]
  (def layout (get-layout (get-layout-type d)))
  (def p (merge payload {:content (render-from-markdown d)}))
  (def prerender (render-from-liquid layout p))
  (merge d {:content prerender})
  (spit
    (str (fs/file site-template-dir output-dir (:name d)) ".html")
    (render d p)))

(write-file (posts "2016-03-14-welcome-to-jekyll"))
