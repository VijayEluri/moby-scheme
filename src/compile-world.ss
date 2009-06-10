#lang scheme/base
(require scheme/gui/base
         scheme/contract
         scheme/class
         scheme/match
         scheme/file
         scheme/runtime-path
         syntax/modresolve
         (only-in xml xexpr->string)
         "helpers.ss"
         "env.ss"
         "image-lift.ss"
         "beginner-to-arduino.ss"
         "utils.ss"
         "template.ss"
         "config.ss"
         "pinfo-arduino.ss" ; TODO: HACK
         "permission.ss")

(provide/contract [generate-j2me-application
                   (string? path-string? path-string? . -> . any)]
                  
                  [generate-android-application
                   (string? path-string? path-string? . -> . any)]
                  
                  [generate-arduino-application
                   (string? path-string? path-string? . -> . any)])


;; A platform is one of PLATFORM:ANDROID, PLATFORM:J2ME.
(define PLATFORM:ANDROID 'android)
(define PLATFORM:J2ME 'j2me)
(define PLATFORM:ARDUINO 'arduino)


;; A stub is one of the following
(define STUB:WORLD 'stub:world)
(define STUB:GUI-WORLD 'stub:gui-world)

;; stub=?: stub stub -> boolean
(define (stub=? stub-1 stub-2)
  (eq? stub-1 stub-2))



;; A program is a (listof sexp).

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define-runtime-path antenna.jar "../support/common/externals/antenna-bin-1.1.0-beta.jar")
(define-runtime-path proguard-home "../support/common/externals/proguard4.2")

(define-runtime-path common-support-src-path "../support/common/src")
(define-runtime-path j2me-support-src-path "../support/j2me/src")
(define-runtime-path j2me-support-res-path "../support/j2me/res")

(define-runtime-path j2me-world-stub-path
  "../support/j2me/MidletStub.java.template")

(define-runtime-path arduino-world-stub-path
  "../support/arduino/stub.pde.template")

(define-runtime-path android-gui-world-stub-path
  "../support/android/ActivityStub.java.template")

(define-runtime-path android-skeleton-path "../support/android/skeleton")
(define-runtime-path arduino-skeleton-path "../support/arduino/skeleton")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;





;; generate-j2me-app: string path path -> void
;; Given a file written in beginner-level scheme, generates a j2me application.
(define (generate-j2me-application name file dest)
  (compile-program-to-j2me (open-beginner-program file) name dest))



;; generate-android-application: string path path -> void
;; Compiles an Android application.
(define (generate-android-application name file dest)
  (compile-program-to-android (open-beginner-program file) name dest))


;; generate-arduino-application: string path path -> void
;; Compiles an Arduino application.
(define (generate-arduino-application name file dest)
  (compile-program-to-arduino (open-beginner-program file) name dest))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; compiled-program: platform text% string path-string -> void
;; Consumes a text, an application name, destination directory, and produces an application.
;; The text buffer is assumed to contain a beginner-level program that uses only the world
;; teachpack.  We need to consume a text because we must first lift up all the images
;; as resources.
(define (compile-program-to-j2me text name dest-dir)
;  (log-info (format "Compiling ~a to ~s" name dest-dir))
  (make-j2me-directories dest-dir)
  (lift-images-to-directory text (build-path dest-dir "res"))
  (let*-values ([(classname)
                 (upper-camel-case name)]
                [(program)
                 (parse-text-as-program text)]
                [(compiled-program) ""] ; HACK
;                 (program->java-string program)]
                [(defns pinfo)
                 (values (compiled-program-defns compiled-program)
                         (compiled-program-pinfo compiled-program))]
                [(mappings) 
                 (build-mappings 
                  (PROGRAM-NAME classname)
                  (PROGRAM-DEFINITIONS defns)
                  (PROGRAM-TOPLEVEL-EXPRESSIONS
                   (compiled-program-toplevel-exprs
                    compiled-program))
                  (ON-START (get-on-start-code pinfo))
                  (ON-PAUSE (get-on-pause-code pinfo))
                  (ON-DESTROY (get-on-destroy-code pinfo)))]
                [(source-path) 
                 (build-path dest-dir "src" "org" "plt" classname (string-append classname ".java"))])
    (fill-template-file j2me-world-stub-path source-path mappings)
    (write-j2me-resources text name dest-dir)
    (run-ant-build.xml dest-dir)))

;; make-directories: path -> void
;; Creates the necessary directories.
(define (make-j2me-directories dest-dir)
  (when (directory-exists? dest-dir)
    (delete-directory/files dest-dir))
  (make-directory* dest-dir)
  (make-directory* (build-path dest-dir "src"))
  (make-directory* (build-path dest-dir "res")))


;; write-source-and-resources: text% path -> void
;; Writes out all the external resources we need.
(define (write-j2me-resources a-text name dest-dir)
  (copy-directory/files* common-support-src-path (build-path dest-dir "src"))
  (copy-directory/files* j2me-support-res-path (build-path dest-dir "res"))
  (copy-directory/files* j2me-support-src-path (build-path dest-dir "src"))
  (write-j2me-ant-buildfile name dest-dir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; compile-program-to-android: text string path -> void
;; Writes out the compilation of the text program with the given name to the
;; destination directory.
(define (compile-program-to-android text name dest-dir)
;  (log-info (format "Compiling ~a to ~s" name dest-dir))
  (make-android-directories dest-dir)
  (lift-images-to-directory text (build-path dest-dir "src"))
  (let* ([classname
          (upper-camel-case name)]
         [program
          (parse-text-as-program text)]
         [compiled-program ""] ; HACK
;          (program->java-string program)]
         [pinfo
          (compiled-program-pinfo compiled-program)]
         [mappings
          (build-mappings 
           (PROGRAM-NAME classname)
           (PROGRAM-DEFINITIONS 
            (compiled-program-defns compiled-program))
           (PROGRAM-TOPLEVEL-EXPRESSIONS
            (compiled-program-toplevel-exprs compiled-program))
           (ON-START (get-on-start-code pinfo))
           (ON-PAUSE (get-on-pause-code pinfo))
           (ON-DESTROY (get-on-destroy-code pinfo)))]
         [source-path
          (build-path dest-dir "src" "org" "plt" classname (string-append classname ".java"))])
    (cond
      [(stub=? (choose-program-stub pinfo) STUB:WORLD)
       (fill-template-file j2me-world-stub-path source-path mappings)
       (write-android:world-resources pinfo name dest-dir)
       (run-ant-build.xml dest-dir)]      

      [(stub=? (choose-program-stub pinfo) STUB:GUI-WORLD)
       ;; fixme!
       (fill-template-file android-gui-world-stub-path source-path mappings)
       (write-android:gui-world-resources pinfo name dest-dir)
       (run-ant-build.xml dest-dir)
       (void)]
      
      [else
       (error 'compile-program-to-android
              "Unrecognized stub ~s"
              (choose-program-stub pinfo))])))


(define (make-android-directories dest-dir)
  (when (directory-exists? dest-dir)
    (delete-directory/files dest-dir))
  (make-directory* dest-dir)
  (copy-directory/files* common-support-src-path (build-path dest-dir "src"))
  ;; At the moment, we use j2me bridge classes.
  (copy-directory/files* j2me-support-src-path (build-path dest-dir "src"))
  (copy-directory/files* android-skeleton-path dest-dir)
  (make-directory* (build-path dest-dir "libs")))


(define (make-arduino-directories dest-dir)
  (when (directory-exists? dest-dir)
    (delete-directory/files dest-dir))
  (make-directory* dest-dir)
  (copy-directory/files* arduino-skeleton-path dest-dir))


(define (write-android:world-resources pinfo a-name dest-dir)
  (let ([mappings (build-mappings (PROGRAM-NAME (upper-camel-case a-name))
                                  (ANDROID-SDK-PATH (current-android-sdk-path))
                                  (ANDROID-TOOLS-PATH (current-android-sdk-tools-path)))])
    (replace-template-file dest-dir "src/j2ab/android/app/J2ABMIDletActivity.java" mappings)
    (write-android-manifest dest-dir 
                            #:name a-name
                            #:permissions (collect-required-android-permissions pinfo))
    (replace-template-file dest-dir "build.xml" mappings)
    (replace-template-file dest-dir "res/values/strings.xml" mappings)
    (replace-template-file dest-dir "src/jad.properties" mappings)))


(define (write-android:gui-world-resources pinfo a-name dest-dir)
  (let* ([classname (upper-camel-case a-name)]
         [mappings (build-mappings (PROGRAM-NAME classname)
                                   (ANDROID-SDK-PATH (current-android-sdk-path))
                                   (ANDROID-TOOLS-PATH (current-android-sdk-tools-path)))])
    (write-android-manifest dest-dir 
                            #:name a-name
                            #:activity-class (string-append
                                              "org.plt." classname "." classname)
                            #:permissions (collect-required-android-permissions pinfo))
    (replace-template-file dest-dir "build.xml" mappings)
    (replace-template-file dest-dir "res/values/strings.xml" mappings)
    (replace-template-file dest-dir "src/jad.properties" mappings)))


;; compile-program-to-arduino: text string path -> void
;; Writes out the compilation of the text program with the given name to the
;; destination directory.
(define (compile-program-to-arduino text name dest-dir)
;  (log-info (format "Compiling ~a to ~s" name dest-dir))
  (make-arduino-directories dest-dir)
  (let*-values ([(program)
                 (parse-text-as-program text)]
                [(compiled-program)
                 (program->arduino-string program)]
                [(defns pinfo)
                 (values (compiled-program-defns compiled-program)
                         (compiled-program-pinfo compiled-program))]
                [(mappings) 
                 (build-mappings 
                  (PROGRAM-DEFINITIONS defns)
                  (PROGRAM-TOPLEVEL-EXPRESSIONS
                   (compiled-program-toplevel-exprs
                    compiled-program)))]
                [(source-path) 
                 (build-path dest-dir (string-append name ".pde"))])
    (fill-template-file arduino-world-stub-path source-path mappings)
    ;(run-ant-build.xml dest-dir)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (replace-template-file dest-dir a-path mappings)
  (fill-template-file (build-path dest-dir (string-append a-path ".template"))
                      (build-path dest-dir a-path)
                        mappings)
  (delete-file (build-path dest-dir (string-append a-path ".template"))))





(define WORLD-PATH-1 (resolve-module-path '(lib "world.ss" "moby" "stub") #f))
(define WORLD-PATH-2 (resolve-module-path '(lib "world.ss" "teachpack" "htdp") #f))
(define GUI-WORLD-PATH (resolve-module-path '(lib "gui-world.ss" "gui-world") #f))


;; choose-program-stub: pinfo -> stub
;; Returns the stub necessary to compile this program.
(define (choose-program-stub a-pinfo)
  (let/ec return
    (for ([b (in-hash-keys (pinfo-used-bindings a-pinfo))])
      (cond
        [(and (binding:function? b)
              (binding:function-module-path b))
         (cond
           [(or (path=? (binding:function-module-path b)
                        WORLD-PATH-1)
                (path=? (binding:function-module-path b)
                        WORLD-PATH-2))
            (return STUB:WORLD)]
           [(path=? (binding:function-module-path b)
                    GUI-WORLD-PATH)
            (return STUB:GUI-WORLD)])]))
      (error 'choose-program-stub "Couldn't identify stub to use for this program.")))


;; collect-required-permissions: pinfo -> (listof symbol)
(define (collect-required-android-permissions a-pinfo)
  (define ht (make-hash))
  (for ([p (get-permissions a-pinfo)])
    (for ([ps (permission->android-permissions p)])
      (hash-set! ht ps #t)))
  (for/list ([p (in-hash-keys ht)])
    p))


;; get-permissions: pinfo -> (listof permission)
(define (get-permissions a-pinfo)
  (define ht (make-hash))
  (for ([b (in-hash-keys (pinfo-used-bindings a-pinfo))])
    (cond
      [(binding:function? b)
       (for ([p (binding:function-permissions b)])
         (hash-set! ht p #t))]))
  (for/list ([p (in-hash-keys ht)])
    p))
  
;; get-on-start-code: pinfo -> string
(define (get-on-start-code a-pinfo)
  (apply string-append
         (map permission->on-start-code (get-permissions a-pinfo))))

;; get-on-pause-code: pinfo -> string
(define (get-on-pause-code a-pinfo)
  (apply string-append
         (map permission->on-pause-code (get-permissions a-pinfo))))

;; get-on-shutdown-code: pinfo -> string
(define (get-on-destroy-code a-pinfo)
  (apply string-append
         (map permission->on-destroy-code (get-permissions a-pinfo))))
  





;; parse-text-as-program: text -> program
;; Given a text, returns a program as well.
(define (parse-text-as-program a-text)
  (let* ([ip (open-input-text-editor a-text)])
    (parameterize ([read-accept-reader #t])
      (let ([s-exp (read ip)])
        (match s-exp
          [(list 'module name lang body ...)
           ;; FIXME: check that the language is beginner level!
           body])))))





;; lift-images: text path -> void
;; Lifts up the image snips in the text, writing them into the resource directory.
;; The snips in the text will be replaced with the expression (create-image <path>)
;; where path refers to the file saves in the resource directory.
(define (lift-images-to-directory a-text resource-dir)
  (make-directory* resource-dir)
  (for ([nb (lift-images! a-text)])
    (named-bitmap-save nb resource-dir)))



;; write-j2me-ant-buildfile: string path -> void
;; Writes a build file that's specialized toward building the midlet.
(define (write-j2me-ant-buildfile name dest-dir 
                                  #:cdlc-version [cldc-version "1.0"]
                                  #:midp-version [midp-version "2.0" #;"1.0"]
                                  )
  (define (property name val)
    `(property ((name ,name) (value ,val))))
  
  (let ([build.xml
         `(project 
           ((name ,(upper-camel-case name)) (default "package"))
           "\n"
           (taskdef ((resource "antenna.properties")
                     (classpath ,(path->string antenna.jar))))
           "\n"
           ,(property "wtk.home" (path->string (current-j2me-home))) "\n"
           ,(property "wtk.proguard.home" (path->string proguard-home)) "\n"
           ,(property "wtk.cldc.version" cldc-version) "\n"
           ,(property "wtk.midp.version" midp-version) "\n"
           ,(property "midlet.name" (upper-camel-case name)) "\n"
           ,(property "company.name" "PLT") "\n"
           "\n"
           (target ((name "init"))
                   (mkdir ((dir "classes")))
                   (mkdir ((dir "bin"))))
           "\n"
           (target ((name "make.jad") (depends "init"))
                   (wtkjad ((jadfile ,(path->string 
                                       (build-path 
                                        "bin" 
                                        (string-append (upper-camel-case name) ".jad"))))
                            (jarfile ,(path->string 
                                       (build-path 
                                        "bin" 
                                        (string-append (upper-camel-case name) ".jar"))))
                            (name "${midlet.name}")
                            (vendor "${company.name}")
                            (version "1.0.0"))
                           (midlet ((name ,(upper-camel-case name))
                                    (class ,(string-append "org.plt." 
                                                           (upper-camel-case name)
                                                           "."
                                                           (upper-camel-case name)))))))
           "\n"
           (target ((name "compile") (depends "init"))
                   (copy ((todir "classes"))
                         (fileset ((dir "src"))
                                  (include ((name "**/*.class")))))
                   (wtkbuild ((srcdir "src")
                              (destdir "classes")
                              (source "1.3")
                              (target "1.3")
                              (preverify "false"))))
           "\n"
           (target ((name "package") (depends "compile,make.jad"))
                   (wtkpackage ((jarfile "bin/${midlet.name}.jar")
                                (jadfile "bin/${midlet.name}.jad")
                                (obfuscate "false")
                                (preverify "true"))
                               (preserve ((class "org.plt.platform.Platform")))
                               (preserve ((class "org.plt.platform.J2MEPlatform")))
                               (fileset ((dir "classes")))
                               (fileset ((dir "res")))))
           "\n"           
           (target ((name "run") (depends "package"))
                   (wtkrun ((jadfile "bin/${midlet.name}.jad")))))])
    (call-with-output-file (build-path dest-dir "build.xml")
      (lambda (op)
        (display (xexpr->string build.xml) op))
      #:exists 'replace)))



;; write-android-manifest: path (#:name string) (#:permissions (listof string)) -> void
(define (write-android-manifest dest-dir
                                #:name name
                                #:activity-class (activity-class 
                                                  "j2ab.android.app.J2ABMIDletActivity")
                                #:permissions (permissions '()))
  (let ([AndroidManifest.xml
         `(manifest 
           ((xmlns:android "http://schemas.android.com/apk/res/android")
            (package ,(string-append "org.plt." (upper-camel-case name)))
            (android:versionCode "1")
            (android:versionName "1.0.0"))

           ,@(map (lambda (p)
                    `(uses-permission ((android:name ,p))))
                  permissions)
           
           (application 
            ((android:label "@string/app_name")
             (android:icon "@drawable/icon"))
            (activity ((android:name ,activity-class)
                       (android:label "@string/app_name"))
                      (intent-filter 
                       ()
                       (action ((android:name "android.intent.action.MAIN")))
                       (category
                        ((android:name
                          "android.intent.category.LAUNCHER")))))))])

    (call-with-output-file (build-path dest-dir "AndroidManifest.xml")
      (lambda (op)
        (display (xexpr->string AndroidManifest.xml) op))
      #:exists 'replace)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; copy-port-to-debug-log: input-port -> void
;; Writes out the lines of the input port as debug events.
(define (copy-port-to-debug-log inp)
  (let loop ([line (read-line inp)])
    (unless (eof-object? line)
;      (log-debug line)
      (loop (read-line inp)))))


;; copy-port-to-error-log: input-port -> void
;; Writes out the lines of the input port as debug events.
(define (copy-port-to-error-log inp)
  (let loop ([line (read-line inp)])
    (unless (eof-object? line)
;      (log-error line)
      (loop (read-line inp)))))


;; run-ant-build.xml: path -> void
;; Runs ant to build the program in the destination directory.
;; Assumes the build file is called "build.xml" at the top of the directory.
(define (run-ant-build.xml dest-dir)
  (parameterize ([current-directory dest-dir])
    (let*-values ([(a-subprocess inp outp errp)
                   (subprocess #f #f #f (current-ant-bin-path))]
                  [(t1 t2) 
                   (values (thread (lambda () 
                                     (copy-port-to-debug-log inp)))
                           (thread (lambda ()
                                     (copy-port-to-error-log errp))))])
      (close-output-port outp)
      (subprocess-wait a-subprocess)
      (sync t1)
      (sync t2)
      (void))))


;; open-beginner-program: path-string -> text%
;; Opens up the beginner-level program.
(define (open-beginner-program path)
  (define text (new text%))
  (send text insert-file (path->string path))
  text)