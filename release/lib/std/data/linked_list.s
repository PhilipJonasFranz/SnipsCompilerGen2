.data
List.ListNode: .word 0
    
List.LinkedList: .word 0
    
.text
    
.global List.LinkedList.add_P_1
List.LinkedList.add_P_1:                     /* Function: List.LinkedList.add, Provisos: INT */
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3-r5, lr}
    mov r3, r0
    mov r4, r1
    push { r4 }
    ldr r0, .POOL0_L0_NULL                   /* Load null address */
    push { r0 }
    ldr r0, .POOL0_List.ListNode
    push { r0 }
    bl init_P_3                              /* Call init */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL0_L0_NULL                   /* Load null address */
    cmp r0, r1
    bne List.LinkedList.add.L1
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #4]                         /* Store value to struct field */
    mov r0, r5                               /* Evaluate Expression */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
    b List.LinkedList.add.L0
List.LinkedList.add.L1: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    str r5, [r1, #4]                         /* Store value to struct field */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
List.LinkedList.add.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    add r1, r1, #12
    ldr r0, [r1]
    add r2, r0, #1
    str r2, [r1]
    ldmfd sp!, {r3-r5, pc}
.POOL0_List.ListNode: .word List.ListNode
.POOL0_L0_NULL: .word NULL
    
.global List.LinkedList.create_P_1
List.LinkedList.create_P_1:                  /* Function: List.LinkedList.create, Provisos: INT */
    push { r3, lr }
    mov r3, r0
    push { r3 }
    mov r0, #0
    ldr r1, .POOL1_L0_NULL                   /* Load null address */
    ldr r2, .POOL1_L0_NULL                   /* Load null address */
    stmfd sp!, {r0-r2}
    ldr r0, .POOL1_List.LinkedList
    push { r0 }
    bl init_P_5                              /* Call init */
    ldmfd sp!, {r3, pc}
.POOL1_List.LinkedList: .word List.LinkedList
.POOL1_L0_NULL: .word NULL
