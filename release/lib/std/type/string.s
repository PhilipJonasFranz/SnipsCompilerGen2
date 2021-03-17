.version 19456431028

.include linked_list.s

.text
.global String.equals
String.equals:                               /* Function: String.equals */
    sub sp, sp, #24
    stmea sp, {r3-r8}
    mov r3, r0
    mov r4, r1
    mov r5, #1
    mov r6, #0
String.equals.L1:                            /* Evaluate condition */
    add r0, r3, r6
    ldr r7, [r10, r0, lsl #2]                /* Load from address */
    add r0, r4, r6
    ldr r8, [r10, r0, lsl #2]                /* Load from address */
    cmp r7, r8
    moveq r0, #1
    movne r0, #0
    adds r0, r0, #0
    movne r0, #1
    cmp r5, #0
    movne r5, r0
    moveq r5, #0
    cmp r7, #0
    moveq r0, #1
    movne r0, #0
    cmp r8, #0
    push { r0 }
    moveq r0, #1
    movne r0, #0
    mov r1, r0
    pop { r0 }
    orrs r0, r0, r1
    movne r0, #1
    moveq r0, #0
    cmp r0, #0
    beq String.equals.L3
    b String.equals.L2
String.equals.L3: 
    mov r0, r6
    add r6, r6, #1
    b String.equals.L1
String.equals.L2: 
    mov r0, r5                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8}
    bx lr
    
.global String.substring
String.substring:                            /* Function: String.substring */
    sub sp, sp, #32
    stmea sp, {r3-r9, lr}
    mov r3, r0
    mov r4, r1
    mov r5, r2
    mov r0, #0                               /* Evaluate Expression */
    bl List.LinkedList.create_P_1            /* Call List.LinkedList.create */
    mov r6, r0
    mov r7, #0
String.substring.L1:                         /* Evaluate condition */
    add r0, r3, r7
    ldr r0, [r10, r0, lsl #2]                /* Load from address */
    cmp r0, #0
    beq String.substring.L2
    cmp r7, r4
    movge r0, #1
    movlt r0, #0
    cmp r7, r5
    push { r0 }
    movlt r0, #1
    movge r0, #0
    mov r1, r0
    pop { r0 }
    adds r1, r1, #0
    movne r1, #1
    cmp r0, #0
    movne r0, r1
    moveq r0, #0
    cmp r0, #0
    beq String.substring.L3
    add r0, r3, r7
    ldr r1, [r10, r0, lsl #2]                /* Load from address */
    mov r0, r6
    bl List.LinkedList.add_P_1               /* Call List.LinkedList.add */
String.substring.L3: 
    mov r0, r7
    add r7, r7, #1
    b String.substring.L1
String.substring.L2: 
    mov r0, r6                               /* Evaluate Expression */
    bl List.LinkedList.size_P_1              /* Call List.LinkedList.size */
    mov r8, r0
    cmp r8, #1
    moveq r0, #1
    movne r0, #0
    mov r1, #0
    push { r0 }
    mov r0, r6
    bl List.LinkedList.get_P_1               /* Call List.LinkedList.get */
    cmp r0, #0
    moveq r0, #1
    movne r0, #0
    mov r1, r0
    pop { r0 }
    adds r1, r1, #0
    movne r1, #1
    cmp r0, #0
    movne r0, r1
    moveq r0, #0
    cmp r0, #0
    beq String.substring.L6
    mov r0, #1                               /* Evaluate Expression */
    bl resv                                  /* Call resv */
    mov r9, r0
    mov r0, #0                               /* Evaluate Expression */
    str r0, [r10, r3, lsl #2]
    mov r0, r6                               /* Load parameters */
    bl free                                  /* Call free */
    mov r0, r9                               /* Evaluate Expression */
    ldmfd sp!, {r3-r9, pc}
String.substring.L6: 
    add r0, r8, #1
    bl resv                                  /* Call resv */
    mov r3, r0
    mov r9, #0
String.substring.L9: 
    cmp r9, r8
    bge String.substring.L10
    mov r0, r6
    mov r1, r9
    bl List.LinkedList.get_P_1               /* Call List.LinkedList.get */
    push { r0 }
    add r0, r3, r9
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    add r9, r9, #1
    b String.substring.L9
String.substring.L10: 
    mov r0, #0                               /* Evaluate Expression */
    push { r0 }
    add r0, r3, r8
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    mov r0, r6                               /* Load parameters */
    bl free                                  /* Call free */
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r9, pc}
    
.global String.length
String.length:                               /* Function: String.length */
    push { r3, r4 }
    mov r3, r0
    mov r4, #0
String.length.L1:                            /* Evaluate condition */
    add r0, r3, r4
    ldr r0, [r10, r0, lsl #2]                /* Load from address */
    cmp r0, #0
    beq String.length.L2
    mov r0, r4
    add r4, r4, #1
    b String.length.L1
String.length.L2: 
    add r0, r4, #1
    ldmfd sp!, {r3, r4}
    bx lr
    
