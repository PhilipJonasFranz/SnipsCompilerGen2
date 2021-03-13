.text
.global foo
foo:                                         /* Function: foo */
    push { lr }
    bl bar                                   /* Call bar */
    mov r1, #2
    mul r0, r1, r0
    pop { pc }
