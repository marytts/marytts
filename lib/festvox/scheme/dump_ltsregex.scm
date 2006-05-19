;;; These are preordained by the LTS building process
(set! lts_context_window_size 4)
(set! lts_context_extra_feats 1)

(define (dump_ltsregex idir)
  (let ((ifd) (rule_index nil))
    (set! lts_pos 0)
    (set! phone_table (list "epsilon"))
    (set! letter_table (list "nothing" "#" "0" 
			     "a" "b" "c" "d" "e" "f" "g" 
			     "h" "i" "j" "k" "l" "m" "n" 
			     "o" "p" "q" "r" "s" "t" "u" 
			     "v" "w" "x" "y" "z"))
    (format t "here\n");
    (mapcar
     (lambda (l)
       (let ((ifd (fopen (path-append 
			  idir 
			  (string-append l ".tree.wfst")) "r")))
	 (format t "doing: %s\n" l)))
     (cdr (cddr letter_table))
     )))

(define (dump_lts_wfst l ifd ofde ofdh lts_pos)
  "(dump_lts_wfst ifd ofde ofdh lts_pos)
Dump the WFST as a byte table to ifd.  Jumps are dumped as
#define's to ofdh so forward references work.  lts_pos is the 
rule position.  Each state is saves as
    feature  value  true_addr  false_addr
Feature and value are single bytes, which addrs are double bytes."
  (let ((state))
    ;; Skip WFST header
    (while (not (string-equal (set! state (readfp ifd)) "EST_Header_End"))
       (if (equal? state (eof-val))
	   (error "eof in lts regex file")))
    (while (not (equal? (set! state (readfp ifd)) (eof-val)))
      (format ofdh "#define LTS_STATE_%s_%d %s\n" 
	      l (car (car state)) 
	      (lts_bytify lts_pos))
      (cond 
       ((string-equal "final" (car (cdr (car state))))
	(set! lts_pos (- lts_pos 1))
	t) ;; do nothing
       ((string-matches (car (car (cdr state))) ".*_.*")
	(format ofde "   %s, %s, %s , %s , \n"
		(lts_feat (car (car (cdr state))))
;		(lts_val (car (car (cdr state))))
		(lts_phone (lts_letter (car (car (cdr state)))) 0 letter_table)
		(format nil "LTS_STATE_%s_%d" l 
			(car (cdr (cdr (car (cdr (cdr state)))))))
		(format nil "LTS_STATE_%s_%d" l 
			(car (cdr (cdr (car (cdr state))))))))
       (t ;; its a letter output state
	(format ofde "   255, %s, 0,0 , 0,0 , \n"
		(lts_phone (car (car (cdr state))) 0 phone_table))))
      (set! lts_pos (+ 1 lts_pos)))
    lts_pos))

(define (lts_feat trans)
  "(lts_feat trans)
Returns the feature number represented in this transition name."
  (let ((fname (substring trans 5 (- (length trans) 11))))
    (if (string-matches fname ".*_i?")
	(set! fname (string-before fname "_")))
    (cond
     ((string-equal fname "p.p.p.p.name") 0)
     ((string-equal fname "p.p.p.name") 1)
     ((string-equal fname "p.p.name") 2)
     ((string-equal fname "p.name") 3)
     ((string-equal fname "n.name") 4)
     ((string-equal fname "n.n.name") 5)
     ((string-equal fname "n.n.n.name") 6)
     ((string-equal fname "n.n.n.n.name") 7)
     (t (error (format nil "ltsregex2C: unknown feat %s %s\n" fname trans ))))))

(define (lts_letter trans)
  "(lts_val trans)
The letter being tested."
  (string-before (string-after trans "is_") "_"))

(define (lts_phone p n table)
  (cond
   ((string-equal p (car table))
    n)
   ((not (cdr table))  ;; new p
    (set-cdr! table (list p))
    (+ 1 n))
   (t
    (lts_phone p (+ 1 n) (cdr table)))))
  
(define (lts_bytify n)
  "(lts_bytify n)
Return this short as a two byte comma separated string."
  (let ((xx (format nil "%04x" n)))
    ;; This is unfortunately byte order specific
    (format nil "0x%s,0x%s"
	    (substring xx 2 2)
	    (substring xx 0 2))))

(provide 'make_lts)
