; Portions Copyright 2004 Sun Microsystems, Inc.
; Portions Copyright 1999-2003 Language Technologies Institute,
; Carnegie Mellon University.
; All Rights Reserved.  Use is subject to license terms.
;
; See the file "license.terms" for information on usage and
; redistribution of this file, and for a DISCLAIMER OF ALL
; WARRANTIES.

; Dumps the clunit_selection_trees to standard out.
;
; Expects clunit_selection_trees to be defined.
;
(define (dump_trees) 
  (mapcar
   (lambda (cart)
     (set! current_node 0)
     (let ((tree (cadr cart))
	   (name (car cart)))
       (set! cart_nodes_text (format nil "%s" (print_cart_nodes tree)))
       (format t "CART %s %d\n" name current_node)
       (format t "%s" cart_nodes_text)
       ))
   clunits_selection_trees))

(defvar cart_operators
  '(("is" "=")
    ("in" "IN")
    ("<" "<")
    (">" ">")
    ("matches" "REGEX")
    ("=" "EQUALS"))) ; CST_CART_OP_EQUALS not handled in
                     ;    Flite->FreeTTS Conversion
                     ; May cause problems.

(define (print_cart_list l)
    (cond
        ((null? l))
        ((cdr l) (format nil "%f,%s" (caar l) (print_cart_list (cdr l))))
        (t (format nil "%f" (caar l)))
))

(define (print_cart_nodes tree)
  (set! current_node (+ 1 current_node))
  (cond
   ((cdr tree) ;node (non-leaf)
    (let ((operator (cadr (assoc_string (cadr (car tree)) cart_operators)))
	  (val (nth 2 (car tree))))
      (let ((type (cond
		   ((string-equal operator "=") (format nil "String(%s)" val))
		   ((string-equal operator "REGEX") (format nil "Integer(%d)" val))
		   ((number? val) (format nil "Float(%f)" val))
		   ((consp val) (format stderr "List vals not supported here yet\n")
		    (error val))
		   (t (format nil "String(%s)" val))
		   )))
	(let ((left_val (print_cart_nodes (car (cdr tree)))))
          (let ((this_node_val (format nil "NODE %s %s %s %d\n"
				       (caar tree) ;feat
				       operator
				       type
				       current_node)))
	    (let ((right_val (print_cart_nodes (car (cdr (cdr tree))))))
	      (string-append this_node_val left_val right_val))))
        )))
   (t (cond
       ((consp (caar tree))    ;leaf = (caar tree)
	(format nil "LEAF List(%s)\n"
                (print_cart_list (caar tree)))
	)
       (t (format stderr "Unknown leaf format\n") (error 1))
       ))))
