.version 0

.text
.global __op_mod
__op_mod:                                    /* Function: __op_mod */
    sub sp, sp, #20
    stmea sp, {r3-r7}
    mov r3, r0
    mov r4, r1
    cmp r0, #0
    bne __op_mod.L0
    mov r0, #0                               /* Evaluate Expression */
    b __op_mod.L14
__op_mod.L0: 
    mov r0, #0                               /* Evaluate Expression */
    mov r5, #0
    cmp r3, #0
    bge __op_mod.L2
    mov r5, #1
    rsb r3, r3, #0
__op_mod.L2: 
    mov r0, r4                               /* Evaluate Expression */
    mov r6, r4
__op_mod.L5:                                 /* Evaluate condition */
    cmp r6, r3
    bgt __op_mod.L6
    lsl r6, r6, #1
    b __op_mod.L5
__op_mod.L6: 
    mov r0, r3                               /* Evaluate Expression */
    mov r7, r3
__op_mod.L8:                                 /* Evaluate condition */
    cmp r7, r4
    blt __op_mod.L9
    lsr r6, r6, #1
    cmp r6, r7
    bgt __op_mod.L10
    sub r7, r7, r6
__op_mod.L10: 
    b __op_mod.L8
__op_mod.L9: 
    cmp r7, #0
    movne r0, #1
    moveq r0, #0
    adds r1, r5, #0
    movne r1, #1
    cmp r0, #0
    movne r0, r1
    moveq r0, #0
    cmp r0, #0
    beq __op_mod.L12
    sub r7, r4, r7
__op_mod.L12: 
    mov r0, r7                               /* Evaluate Expression */
__op_mod.L14: 
    ldmfd sp!, {r3-r7}
    bx lr
