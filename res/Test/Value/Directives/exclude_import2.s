.version -1529223909

.include res\Test\Value\Directives\exclude_import3.s

.text

.global foo
foo:                                         /* Function: foo */
    push { lr }
    bl bar                                   /* Call bar */
    mov r1, #2
    mul r0, r1, r0
    pop { pc }
