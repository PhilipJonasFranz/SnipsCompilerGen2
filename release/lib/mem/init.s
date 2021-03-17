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

.global init_P_2
init_P_2: 
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #2
    bl resv
    mov r3, r0
    ldr r0, [fp, #16]
    ldr r1, [fp, #12]
    stmfd sp!, {r0, r1}
    pop { r0 }
    lsl r1, r3, #2
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    mov r0, r3
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #8
    bx lr

.global init_P_7
init_P_7: 
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #7
    bl resv
    mov r3, r0
    add r0, fp, #36
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    add r0, fp, #24
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    ldr r0, [fp, #12]
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
    pop { r0 }
    str r0, [r1, #20]
    pop { r0 }
    str r0, [r1, #24]
    mov r0, r3
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #28
    bx lr

.global init_P_1
init_P_1: 
    sub sp, sp, #12
    stmea sp, {r3, r4, lr}
    mov r3, r0
    mov r0, #1
    bl resv
    mov r4, r0
    str r3, [r10, r4, lsl #2]
    mov r0, r4
    ldmfd sp!, {r3, r4, pc}

.global init_P_4
init_P_4: 
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #4
    bl resv
    mov r3, r0
    add r0, fp, #24
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    ldr r0, [fp, #12]
    lsl r1, r3, #2
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    pop { r0 }
    str r0, [r1, #8]
    pop { r0 }
    str r0, [r1, #12]
    mov r0, r3
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #16
    bx lr

.global init_P_6
init_P_6: 
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #6
    bl resv
    mov r3, r0
    add r0, fp, #32
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
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
    pop { r0 }
    str r0, [r1, #12]
    pop { r0 }
    str r0, [r1, #16]
    pop { r0 }
    str r0, [r1, #20]
    mov r0, r3
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #24
    bx lr

.global init_P_8
init_P_8: 
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r0, #8
    bl resv
    mov r3, r0
    add r0, fp, #40
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
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
    pop { r0 }
    str r0, [r1, #20]
    pop { r0 }
    str r0, [r1, #24]
    pop { r0 }
    str r0, [r1, #28]
    mov r0, r3
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    add sp, sp, #32
    bx lr
