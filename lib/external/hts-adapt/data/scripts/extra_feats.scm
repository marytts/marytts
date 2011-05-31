;; ----------------------------------------------------------------- ;;
;;           The HMM-Based Speech Synthesis System (HTS)             ;;
;;           developed by HTS Working Group                          ;;
;;           http://hts.sp.nitech.ac.jp/                             ;;
;; ----------------------------------------------------------------- ;;
;;                                                                   ;;
;;  Copyright (c) 2001-2011  Nagoya Institute of Technology          ;;
;;                           Department of Computer Science          ;;
;;                                                                   ;;
;;                2001-2008  Tokyo Institute of Technology           ;;
;;                           Interdisciplinary Graduate School of    ;;
;;                           Science and Engineering                 ;;
;;                                                                   ;;
;; All rights reserved.                                              ;;
;;                                                                   ;;
;; Redistribution and use in source and binary forms, with or        ;;
;; without modification, are permitted provided that the following   ;;
;; conditions are met:                                               ;;
;;                                                                   ;;
;; - Redistributions of source code must retain the above copyright  ;;
;;   notice, this list of conditions and the following disclaimer.   ;;
;; - Redistributions in binary form must reproduce the above         ;;
;;   copyright notice, this list of conditions and the following     ;;
;;   disclaimer in the documentation and/or other materials provided ;;
;;   with the distribution.                                          ;;
;; - Neither the name of the HTS working group nor the names of its  ;;
;;   contributors may be used to endorse or promote products derived ;;
;;   from this software without specific prior written permission.   ;;
;;                                                                   ;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            ;;
;; CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       ;;
;; INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          ;;
;; MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          ;;
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS ;;
;; BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          ;;
;; EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   ;;
;; TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     ;;
;; DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ;;
;; ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   ;;
;; OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    ;;
;; OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           ;;
;; POSSIBILITY OF SUCH DAMAGE.                                       ;;
;; ----------------------------------------------------------------- ;;
;;
;;  Extra features
;;  From Segment items refer by 
;;
;;  R:SylStructure.parent.parent.R:Phrase.parent.lisp_num_syls_in_phrase
;;  R:SylStructure.parent.parent.R:Phrase.parent.lisp_num_words_in_phrase
;;  lisp_total_words
;;  lisp_total_syls
;;  lisp_total_phrases
;;
;;  The last three will act on any item

(define (distance_to_p_content i)
  (let ((c 0) (rc 0 ) (w (item.relation.prev i "Phrase")))
    (while w
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "contentp"))
      (begin
        (set! rc c)
        (set! w nil))
      (set! w (item.prev w)))
      )
    rc))

(define (distance_to_n_content i)
  (let ((c 0) (rc 0) (w (item.relation.next i "Phrase")))
    (while w
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "contentp"))
      (begin
        (set! rc c)
        (set! w nil))
      (set! w (item.next w)))
      )
    rc))

(define (distance_to_p_accent i)
  (let ((c 0) (rc 0 ) (w (item.relation.prev i "Syllable")))
    (while (and w (member_string (item.feat w "syl_break") '("0" "1")))
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "accented"))
      (begin
        (set! rc c)
        (set! w nil))
        (set! w (item.prev w)))
        )
        rc))

(define (distance_to_n_accent i)
  (let ((c 0) (rc 0 ) (w (item.relation.next i "Syllable")))
    (while (and w (member_string (item.feat w "p.syl_break") '("0" "1")))
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "accented"))
      (begin
        (set! rc c)
        (set! w nil))
        (set! w (item.next w)))
        )
        rc))

(define (distance_to_p_stress i)
  (let ((c 0) (rc 0 ) (w (item.relation.prev i "Syllable")))
    (while (and w (member_string (item.feat w "syl_break") '("0" "1")))
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "stress"))
      (begin
        (set! rc c)
        (set! w nil))
        (set! w (item.prev w)))
        )
        rc))

(define (distance_to_n_stress i)
  (let ((c 0) (rc 0 ) (w (item.relation.next i "Syllable")))
    (while (and w (member_string (item.feat w "p.syl_break") '("0" "1")))
      (set! c (+ 1 c))
      (if (string-equal "1" (item.feat w "stress"))
      (begin
        (set! rc c)
        (set! w nil))
        (set! w (item.next w)))
        )
        rc))

(define (num_syls_in_phrase i)
  (apply 
   +
   (mapcar
    (lambda (w)
      (length (item.relation.daughters w 'SylStructure)))
    (item.relation.daughters i 'Phrase))))

(define (num_words_in_phrase i)
  (length (item.relation.daughters i 'Phrase)))

(define (total_words w)
  (length
   (utt.relation.items (item.get_utt w) 'Word)))

(define (total_syls s)
  (length
   (utt.relation.items (item.get_utt s) 'Syllable)))

(define (total_phrases s)
  (length
   (utt.relation_tree (item.get_utt s) 'Phrase)))
