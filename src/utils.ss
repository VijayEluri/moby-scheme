#lang scheme/base
(require scheme/contract
         scheme/file
         (prefix-in 19: srfi/19))

(provide/contract [copy-directory/files* (path-string? path-string? . -> . any)]
                  [upper-camel-case (string? . -> . string?)]
                  [make-temporary-directory (() (#:parent-directory path?) . ->* . path?)]
                  [get-file-bytes (path? . -> . bytes?)]
                  [get-input-port-bytes (input-port? . -> . bytes?)]
                  [now-date-string (-> string?)]
                  [string->date (string? . -> . date?)])


;; copy-directory/files*: path path -> void
;; Like copy-directory/files, but overwrites rather than raises exn:fail:filesystem.
(define (copy-directory/files* from-path dest-path)
  (for ([entry (directory-list from-path)])    
    (cond [(file-exists? (build-path from-path entry))
           ;; Plain file
           (when (file-exists? (build-path dest-path entry))
             (delete-file (build-path dest-path entry)))
           (copy-file (build-path from-path entry)
                      (build-path dest-path entry))]
          [else
           ;; Directory
           ;; Degenerate case: if there's a plain file with the same name, wipe
           ;; it out.
           (when (file-exists? (build-path dest-path entry))
             (delete-file (build-path dest-path entry)))
           ;; Otherwise, recur by going into that directory.
           (when (not (directory-exists? (build-path dest-path entry)))
             (make-directory* (build-path dest-path entry)))
           (copy-directory/files* (build-path from-path entry)
                                  (build-path dest-path entry))])))


;; get-file-bytes: path -> bytes
;; Sucks all the bytes out of a file
(define (get-file-bytes a-path)
  (call-with-input-file a-path
    get-input-port-bytes))

;; get-input-port-bytes: input-port -> bytes
(define (get-input-port-bytes ip)
  (let loop ([b (bytes)])
    (let ([chunk (read-bytes 8196 ip)])
      (cond
        [(eof-object? chunk)
         b]
        [else
         (loop (bytes-append b chunk))]))))
  



;; make-temporary-directory: -> path
;; Creates a temporary directory, and returns its path.
(define (make-temporary-directory #:parent-directory (parent-dir #f))
  (let ([f (make-temporary-file "tmp~a" #f parent-dir)])
    (delete-file f)
    (make-directory f)
    f))





;; now-date-string: -> string
;; Returns the current date as a string.
(define (now-date-string)
  (19:date->string (19:current-date) "~5"))

;; string->date: string -> date
(define (string->date an-str)
  (let* ([d (19:string->date an-str "~Y-~m-~dT~H:~M:~S")]
         [second (19:date-second d)]
         [minute (19:date-minute d)]
         [hour (19:date-hour d)]
         [day (19:date-day d)]
         [month (19:date-month d)]
         [year (19:date-year d)]
         [week-day (19:date-week-day d)]
         [year-day (19:date-year-day d)]
         [dst? #f] ;; fixme: I have no idea how to get daylight savings time from SRFI 19
         [time-zone-offset (19:date-zone-offset d)])
    (make-date second
               minute
               hour
               day
               month
               year
               week-day
               year-day
               dst?
               time-zone-offset)))
         

;; upper-camel-case: string -> string
;; Given a string name, perform an UpperCamelCasing of the name.
(define (upper-camel-case name)
  (apply string-append
         (map string-titlecase (regexp-split #px"[\\s]+" 
                                             (regexp-replace* #px"[^\\s\\w]+" name " ")))))

;; mapi: (a -> b) -> i -> list[a] -> list[b]
(define (mapi f i l)
  (cond
    [(null? l) '()]
    [(pair? l) (cons (f i (car l)) (mapi f (+ i 1) (cdr l)))]))