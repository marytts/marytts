; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps the given compiled lexicon file to stdout.
;
(define (dump_lex compiled_lex)
     (let ((ifd (fopen compiled_lex "r")))
       (if (not (string-equal "MNCL" (readfp ifd)))
	   (error "dump_lex: input file is not a compiled lexicon\n"))
       (while (not (equal? (set! entry (readfp ifd)) (eof-val)))
	      ;; Determine part of speech character.
	      ;;
	      (if (not (car (cdr entry)))
		  (set! pos "0")
		  (set! pos (substring 
			     (string-append (car (cdr entry))) 0 1)))
	      (format t "%s%s\t"  (car entry) pos)

	      ;; Dump the phones
	      ;;
	      (let ((syllables (caddr entry)))
		(while syllables 
		       (let ((syllable (car syllables)))
			 (let ((phones (car syllable)))
			   (while phones 
				  (if (and (is_a_vowel (car phones))
					   (equal? 1 (car (cdr syllable))))
				      (format t "%s1 " (car phones))
				      (format t "%s " (car phones)))
				  (set! phones (cdr phones)))))
		(set! syllables (cdr syllables))))

	      (format t "\n"))
       (fclose ifd)))

;; Should be a better way to do this
(set! vowels
      '(
	;; radio (CMULEX)
	aa ae ah ao aw ax axr ay eh el em en er ey ih iy ow oy uh uw
        ;; mrpa (OALD)
	uh e a o i u ii uu oo aa @@ ai ei oi au ou e@ i@ u@ @
	;; ogi_worldbet
        i: I E @ u U ^ & > A 3r ei aI >i iU aU oU 
        ))

(define (is_a_vowel p)
  (member_string p vowels))
