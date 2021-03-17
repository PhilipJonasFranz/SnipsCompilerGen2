.version 2750349890

.text
    
.global init_P_5
init_P_5:                                    /* Function: init, Provisos: List.LinkedList<INT> |  */
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #5                               /* Evaluate Expression */
    bl resv                                  /* Call resv */
    mov r3, r0
    add r0, fp, #28
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    ldr r0, [fp, #16]
    ldr r1, [fp, #12]
    stmfd sp!, {r0, r1}
    pop { r0 }
    lsl r1, r3, #2
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    pop { r0 }
    str r0, [r1, #8]
    pop { r0 }
    str r0, [r1, #12]
    pop { r0 }
    str r0, [r1, #16]
    mov r0, r3                               /* Evaluate Expression */
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #20
    bx lr
    
.global init_P_3
init_P_3:                                    /* Provisos: List.ListNode<INT> */
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #3                               /* Evaluate Expression */
    bl resv                                  /* Call resv */
    mov r3, r0
    add r0, fp, #20
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    pop { r0 }
    lsl r1, r3, #2
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    pop { r0 }
    str r0, [r1, #8]
    mov r0, r3                               /* Evaluate Expression */
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #12
    bx lr
