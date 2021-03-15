.include collection.s
.include iterable.s
    
.data
Streamable.Stream: .word 0
    
.text
    
.global Streamable.Stream.forEach_P_1
Streamable.Stream.forEach_P_1:               /* Function: Streamable.Stream.forEach, Provisos: INT */
    sub sp, sp, #20
    stmea sp, {r3-r6, lr}
    mov r3, r0
    mov r4, r1
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    mov r5, r0
    mov r6, #0
Streamable.Stream.forEach.L1: 
    cmp r6, r5
    bge Streamable.Stream.forEach.L2
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    mov r1, r6
    mov r12, #4
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    add lr, pc, #8
    mov pc, r4
    push { r0 }
    push { r6 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldmfd sp!, {r1, r2}
    mov r12, #8
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    add r6, r6, #1
    b Streamable.Stream.forEach.L1
Streamable.Stream.forEach.L2: 
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r6, pc}
    
.global Streamable.Stream.filter_P_1
Streamable.Stream.filter_P_1:                /* Function: Streamable.Stream.filter, Provisos: INT */
    sub sp, sp, #24
    stmea sp, {r3-r7, lr}
    mov r3, r0
    mov r4, r1
    mov r5, #0
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    mov r6, r0
    mov r7, #0
Streamable.Stream.filter.L1: 
    cmp r7, r6
    bge Streamable.Stream.filter.L2
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    mov r1, r7
    mov r12, #4
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    add lr, pc, #8
    mov pc, r4
    cmp r0, #0
    moveq r0, #1
    movne r0, #0
    cmp r0, #0
    beq Streamable.Stream.filter.L3
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    mov r1, r5
    mov r12, #16
    bl Collection.L5_P_1                     /* Branch to relay table of Collection */
    sub r5, r5, #1
Streamable.Stream.filter.L3: 
    mov r0, r5
    add r5, r5, #1
    add r7, r7, #1
    b Streamable.Stream.filter.L1
Streamable.Stream.filter.L2: 
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r7, pc}
    
.global Streamable.Stream.create_P_1
Streamable.Stream.create_P_1:                /* Function: Streamable.Stream.create, Provisos: INT */
    push { fp }
    mov fp, sp
    push { r0 }
    ldr r0, .POOL@1790932357_0_Streamable.Stream
    push { r0 }
    mov r2, sp
    mov sp, fp
    pop { fp }
    mov r0, #8
    add r1, r2, #8
    add r10, pc, #8                          /* Setup return address for routine */
    b _routine_stack_copy_
    mov r10, #0
    bx lr
.POOL@1790932357_0_Streamable.Stream: .word Streamable.Stream
