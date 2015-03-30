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

(define (getssq item fp wnm)
  (if item
   (begin
     (set! wnm1 (item.feat item "R:SylStructure.parent.parent.name"))

     (if (not (string-equal wnm1 wnm))
       (if (and (not (equal? wnm 0)) (not (equal? wnm1 0)))
         (format fp "ssil "))) 

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

