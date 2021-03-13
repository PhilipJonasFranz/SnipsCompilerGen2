.text
    
.global init_P_2
init_P_2:                                    /* Function: init, Provisos: B<INT> */
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #2                               /* Evaluate Expression */
    bl resv                                  /* Call resv */
    mov r3, r0
    ldr r0, [fp, #16]                        /* Evaluate Expression */
    ldr r1, [fp, #12]
    stmfd sp!, {r0, r1}
    pop { r0 }
    lsl r1, r3, #2
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    mov r0, r3                               /* Evaluate Expression */
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #8
    bx lr
