.text
.global __op_div
__op_div:                                    /* Function: __op_div */
    sub sp, sp, #16
    stmea sp, {r3-r6}
    mov r3, r0
    mov r4, r1
    mov r5, #0
    mov r6, #1
__op_div.L1:                                 /* Evaluate condition */
    cmp r3, r4
    blt __op_div.L2
__op_div.L4:                                 /* Evaluate condition */
    lsl r0, r4, #1
    cmp r0, r3
    bge __op_div.L5
    lsl r4, r4, #1
    lsl r6, r6, #1
    b __op_div.L4
__op_div.L5: 
    sub r3, r3, r4
    add r5, r5, r6
__op_div.L7:                                 /* Evaluate condition */
    cmp r4, r3
    movgt r0, #1
    movle r0, #0
    cmp r6, #1
    push { r0 }
    movgt r0, #1
    movle r0, #0
    mov r1, r0
    pop { r0 }
    adds r1, r1, #0
    movne r1, #1
    cmp r0, #0
    movne r0, r1
    moveq r0, #0
    cmp r0, #0
    beq __op_div.L8
    lsr r4, r4, #1
    lsr r6, r6, #1
    b __op_div.L7
__op_div.L8: 
    b __op_div.L1
__op_div.L2: 
    mov r0, r5                               /* Evaluate Expression */
    ldmfd sp!, {r3-r6}
    bx lr