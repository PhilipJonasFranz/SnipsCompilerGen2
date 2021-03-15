.include res\Test\Value\Directives\exclude_mult.s
.include res\Test\Value\Directives\exclude_import3.s
    
.text
.global mult
mult:                                        /* Function: mult */
    sub sp, sp, #28
    stmea sp, {r3-r8, fp}
    mov fp, sp
    mov r3, r0
    mov r4, r1
    mov r0, #0                               /* Evaluate Expression */
    mov r1, #0
    stmfd sp!, {r0, r1}
    mov r0, #0
    mov r1, #0
    stmfd sp!, {r0, r1}
    mov r0, #0                               /* Evaluate Expression */
    mov r5, #0
mult.L1: 
    cmp r5, #2
    bge mult.L2
    mov r0, #0                               /* Evaluate Expression */
    mov r6, #0
mult.L4: 
    cmp r6, #2
    bge mult.L5
    mov r7, #0
    mov r0, #0                               /* Evaluate Expression */
    mov r8, #0
mult.L7: 
    cmp r8, #2
    bge mult.L8
    mov r2, #0                               /* Evaluate Expression */
    lsl r0, r5, #3
    add r2, r2, r0
    lsl r0, r8, #2
    add r2, r2, r0
    add r0, fp, #44
    ldr r0, [r0, r2]
    mov r2, #0                               /* Calculate offset of sub structure */
    mov r1, r0
    lsl r0, r8, #3
    add r2, r2, r0
    lsl r0, r6, #2
    add r2, r2, r0
    add r0, fp, #28
    ldr r2, [r0, r2]
    mul r2, r1, r2
    add r7, r7, r2
    add r8, r8, #1
    b mult.L7
mult.L8: 
    mov r2, #0                               /* Calculate offset of sub structure */
    lsl r0, r5, #3
    add r2, r2, r0
    lsl r0, r6, #2
    add r2, r2, r0
    sub r0, fp, #16
    str r7, [r0, r2]
    add r6, r6, #1
    b mult.L4
mult.L5: 
    add r5, r5, #1
    b mult.L1
mult.L2: 
    mov r2, #0                               /* Evaluate Expression */
    lsl r0, r3, #3
    add r2, r2, r0
    lsl r0, r4, #2
    add r2, r2, r0
    sub r0, fp, #16
    ldr r0, [r0, r2]
    mov sp, fp
    ldmfd sp!, {r3-r8, fp}
    add sp, sp, #32
    bx lr
