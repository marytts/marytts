;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                                                                     ;;;
;;;                     Carnegie Mellon University                      ;;;
;;;                         Copyright (c) 2005                          ;;;
;;;                        All Rights Reserved.                         ;;;
;;;                                                                     ;;;
;;; Permission is hereby granted, free of charge, to use and distribute ;;;
;;; this software and its documentation without restriction, including  ;;;
;;; without limitation the rights to use, copy, modify, merge, publish, ;;;
;;; distribute, sublicense, and/or sell copies of this work, and to     ;;;
;;; permit persons to whom this work is furnished to do so, subject to  ;;;
;;; the following conditions:                                           ;;;
;;;  1. The code must retain the above copyright notice, this list of   ;;;
;;;     conditions and the following disclaimer.                        ;;;
;;;  2. Any modifications must be clearly marked as such.               ;;;
;;;  3. Original authors' names are not deleted.                        ;;;
;;;  4. The authors' names are not used to endorse or promote products  ;;;
;;;     derived from this software without specific prior written       ;;;
;;;     permission.                                                     ;;;
;;;                                                                     ;;;
;;; CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK        ;;;
;;; DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING     ;;;
;;; ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT  ;;;
;;; SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE     ;;;
;;; FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES   ;;;
;;; WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN  ;;;
;;; AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,         ;;;
;;; ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF      ;;;
;;; THIS SOFTWARE.                                                      ;;;
;;;                                                                     ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defvar ofile "ehmm/etc/txt.phseq.data")

(define (getwdur item fp)
  (if item
   (begin
     (format fp "%s %f %s\n" (item.name item) (item.feat item 'word_duration) (item.feat item 'pos))
     (getwdur (item.next item) fp))))

;(load "festival/trees/cmu_us_rms.tree")
;(set! cmu_us_rms_unit_tree clunits_selection_trees)
(define (find_unit_name_from_tree name i)
  ;; For ph tag with clunit cluster experiments
  (let ((ttt (cadr (assoc_string name cmu_us_rms_unit_tree)))
        (nn))
    (if ttt
        (set! nn (string-append 
                  name "_"
                  (car (caar (wagon i ttt)))))
        (set! nn name))
;    (format t "name %s nn %s\n" name nn)
    nn))
(define (is_pau i)
  (if (phone_is_silence (item.name i))
      "1"
      "0"))

(define (getssq item fp wnm)
  (if item
   (begin
     (set! wnm1 (item.feat item "R:SylStructure.parent.parent.name"))

     (if (not (string-equal wnm1 wnm))
       (if (and (not (equal? wnm 0)) (not (equal? wnm1 0)))
         (format fp "ssil "))) 
     ;; Use clunit cluster to tag phone
;     (if (string-equal (item.name item) "pau")
;         (format fp "%s " (item.name item))
;         (format fp "%s " (find_unit_name_from_tree (item.name item) item)))

;;      ;; 0 1 or 9 for prominence labeling
;;      (if (member_string
;;           (item.feat item "name") 
;;           '("@" "@@r" "a" "aa" "ai" "e" "ei" "eir" "el" "em" "en" "i" 
;;             "i@" "ii" "iy" "o" "oi" "oo" "ou" "ow" "u" "uh" "ur" "uu" "uw"))
;;          (begin
;;            (cond
;;             ((string-equal (item.feat item "R:SylStructure.parent.stress") "0")
;;              (format fp "%s0 " (item.name item)))
;;             ((and (string-equal (item.feat item "R:SylStructure.parent.accented") "1")
;;                   (string-equal (item.feat item "R:SylStructure.parent.stress") "1"))
;;              (format fp "%s9 " (item.name item)))
;;             (t
;;              (format fp "%s1 " (item.name item))))
;;            )
;;          (format fp "%s " (item.name item))) ;; non-vowel
     ;; context one
;     (if (item.prev item)
;         (set! pn (item.feat item "p.name"))
;         (set! pn "_"))
;     (if (string-equal pn "pau")
;         (set! pn "_"))
;     (if (item.next item)
;         (set! nn (item.feat item "n.name"))
;         (set! nn "_"))
;     (if (string-equal nn "pau")
;         (set! nn "_"))
;     (if (string-equal (item.name item) "pau")
;         (format fp "%s " (item.name item))
;         (format fp "%s%s%s " pn (item.name item) nn))

     ;; Normal one
     (format fp "%s " (item.name item))

     (set! wnm (item.feat item "R:SylStructure.parent.parent.name"))
     (getssq (item.next item) fp wnm))))

(define (proc_file fid)

    (set! fd (fopen ofile "a"))
    (set! u1 (utt.load nil (format nil "prompt-utt/%s.utt" fid)))

    ;;(set! w1 (utt.relation.first u1 'Word))

    (set! w1 (utt.relation.first u1 'Segment))

    ;;(getwdur w1 fd)
    ;;(format fd "%s " bnm) 

    (format fd "%s " fid) 

    (set! wnm (item.feat w1 "R:SylStructure.parent.parent.name"))
    (getssq w1 fd wnm)
    (format fd "\n") 
    (fclose fd)
)

(define (phseq datafile outfile)
  (set! ofile outfile)
  (set! fd1 (fopen ofile "w"))  ;; making it empty
  (fclose fd1)
  (mapcar
   (lambda (f)
     (format t "phseq %s\n" (car f))
     (unwind-protect
      (proc_file (car f))
      nil)
     )
   (load datafile t))
  t)

