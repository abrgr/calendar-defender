(ns calendar-defender.containers.flow-macros)

(defmacro handler-0 [body]
  (let [invocation (list 'clj->js (conj body 'do))]
    (list 'fn []
       invocation)))
(defmacro handler-1 [event & body]
  (let [invocation (list 'clj->js (conj body 'do))
        event# (gensym "event")]
    (list 'fn [event#]
       (list 'let [event (list 'js->clj event# :keywordize-keys true)]
         invocation))))
(defmacro handler-2 [event data & body]
  (let [invocation (list 'clj->js (conj body 'do))
        event# (gensym "event")
        data# (gensym "data")]
    (list 'fn [event# data#]
       (list 'let [event (list 'js->clj event# :keywordize-keys true)
                  data (list 'js->clj data# :keywordize-keys true)]
         invocation))))
(defmacro handler-3 [event data id & body]
  (let [invocation (list 'clj->js (conj body 'do))
        event# (gensym "event")
        data# (gensym "data")]
  (list 'fn [event# data# id]
     (list 'let [event (list 'js->clj event# :keywordize-keys true)
                 data (list 'js->clj data# :keywordize-keys true)]
       invocation))))
