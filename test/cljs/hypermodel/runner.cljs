(ns hypermodel.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [hypermodel.core-test]))

(doo-tests 'hypermodel.core-test)
