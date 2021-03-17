.version -4342759661

.include serializable.s
.include collection.s

.data
List.LinkedList: .word 0
List.ListNode: .word 0

.text
.global List.LinkedList.add_P_1
List.LinkedList.add_P_1:                     /* Function: List.LinkedList.add, Provisos: INT */
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3-r5, lr}
    mov r3, r0
    mov r4, r1
    push { r4 }
    ldr r0, .P1974009529_NULL                /* Load null address */
    push { r0 }
    ldr r0, .P1974009529_List.ListNode
    push { r0 }
    bl init_P_3                              /* Call init */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .P1974009529_NULL                /* Load null address */
    cmp r0, r1
    bne List.LinkedList.add_P_1.L1
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #4]                         /* Store value to struct field */
    mov r0, r5                               /* Evaluate Expression */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
    b List.LinkedList.add_P_1.L0
List.LinkedList.add_P_1.L1: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    str r5, [r1, #4]                         /* Store value to struct field */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
List.LinkedList.add_P_1.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    add r1, r1, #12
    ldr r0, [r1]
    add r2, r0, #1
    str r2, [r1]
    ldmfd sp!, {r3-r5, pc}
.P1974009529_List.ListNode: .word List.ListNode
.P1974009529_NULL: .word NULL

.global List.LinkedList.get_P_1
List.LinkedList.get_P_1:                     /* Function: List.LinkedList.get, Provisos: INT */
    mov r10, #0
    sub sp, sp, #12
    stmea sp, {r3-r5}
    mov r3, r0
    mov r4, r1
    cmp r1, #0
    bge List.LinkedList.get_P_1.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #16]                        /* Load field from struct */
    b List.LinkedList.get_P_1.L7
List.LinkedList.get_P_1.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r5, [r1, #4]                         /* Load field from struct */
List.LinkedList.get_P_1.L3:                  /* Evaluate condition */
    cmp r4, #0
    beq List.LinkedList.get_P_1.L4
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .P1974009528_NULL                /* Load null address */
    cmp r0, r1
    bne List.LinkedList.get_P_1.L5
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #16]                        /* Load field from struct */
    b List.LinkedList.get_P_1.L7
List.LinkedList.get_P_1.L5: 
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r5, [r1, #4]                         /* Load field from struct */
    mov r0, r4
    sub r4, r4, #1
    b List.LinkedList.get_P_1.L3
List.LinkedList.get_P_1.L4: 
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
List.LinkedList.get_P_1.L7: 
    ldmfd sp!, {r3-r5}
    bx lr
.P1974009528_NULL: .word NULL

.global List.LinkedList.create_P_1
List.LinkedList.create_P_1:                  /* Function: List.LinkedList.create, Provisos: INT */
    push { r3, lr }
    mov r3, r0
    push { r3 }
    mov r0, #0
    ldr r1, .P1974009527_NULL                /* Load null address */
    ldr r2, .P1974009527_NULL                /* Load null address */
    stmfd sp!, {r0-r2}
    ldr r0, .P1974009527_List.LinkedList
    push { r0 }
    bl init_P_5                              /* Call init */
    ldmfd sp!, {r3, pc}
.P1974009527_List.LinkedList: .word List.LinkedList
.P1974009527_NULL: .word NULL

.global List.LinkedList.clear_P_1
List.LinkedList.clear_P_1: 
    sub sp, sp, #12
    stmea sp, {r3, r4, lr}
    mov r3, r0
    lsl r1, r0, #2
    ldr r4, [r1, #4]
List.LinkedList.clear_P_1.L1: 
    ldr r1, .P1974009527_NULL
    cmp r4, r1
    beq List.LinkedList.clear_P_1.L2
    mov r0, r4
    bl free
    lsl r1, r4, #2
    ldr r4, [r1, #4]
    b List.LinkedList.clear_P_1.L1
List.LinkedList.clear_P_1.L2: 
    ldr r0, .P1974009527_NULL
    lsl r1, r3, #2
    str r0, [r1, #4]
    ldr r0, .P1974009527_NULL
    lsl r1, r3, #2
    str r0, [r1, #8]
    ldmfd sp!, {r3, r4, pc}
.P1974009527_NULL: .word NULL

.global List.LinkedList.add_P_3
List.LinkedList.add_P_3: 
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3, r4, fp, lr}
    mov fp, sp
    mov r3, r0
/* Initialize the new List Node, set the next pointer to null */
    add r0, fp, #24
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    ldr r0, .P1974009529_NULL
    push { r0 }
    ldr r0, .P1974009529_List.ListNode
    push { r0 }
    bl init_P_5
    mov r4, r0
    lsl r1, r3, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009529_NULL
    cmp r0, r1
    bne List.LinkedList.add_P_3.L1
    lsl r1, r3, #2
    str r4, [r1, #4]
    mov r0, r4
    lsl r1, r3, #2
    str r4, [r1, #8]
    b List.LinkedList.add_P_3.L0
List.LinkedList.add_P_3.L1: 
    lsl r1, r3, #2
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    str r4, [r1, #4]
    lsl r1, r3, #2
    str r4, [r1, #8]
List.LinkedList.add_P_3.L0: 
    lsl r1, r3, #2
    add r1, r1, #12
    ldr r0, [r1]
    add r2, r0, #1
    str r2, [r1]
    mov sp, fp
    ldmfd sp!, {r3, r4, fp, lr}
    add sp, sp, #12
    bx lr
.P1974009529_List.ListNode: .word List.ListNode
.P1974009529_NULL: .word NULL

.global List.LinkedList.get_P_3
List.LinkedList.get_P_3: 
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3-r5, fp}
    mov fp, sp
    mov r3, r0
    mov r4, r1
    cmp r1, #0
    bge List.LinkedList.get_P_3.L0
    lsl r1, r3, #2
    add r1, r1, #16
    ldr r0, [r1, #8]
    ldr r2, [r1, #4]
    stmfd sp!, {r0, r2}
    ldr r0, [r1]
    push { r0 }
    b List.LinkedList.get_P_3.L7
List.LinkedList.get_P_3.L0: 
    lsl r1, r3, #2
    ldr r5, [r1, #4]
List.LinkedList.get_P_3.L3: 
    cmp r4, #0
    beq List.LinkedList.get_P_3.L4
    lsl r1, r5, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009528_NULL
    cmp r0, r1
    bne List.LinkedList.get_P_3.L5
    lsl r1, r3, #2
    add r1, r1, #16
    ldr r0, [r1, #8]
    ldr r2, [r1, #4]
    stmfd sp!, {r0, r2}
    ldr r0, [r1]
    push { r0 }
    b List.LinkedList.get_P_3.L7
List.LinkedList.get_P_3.L5: 
/* Jump to next */
    lsl r1, r5, #2
    ldr r5, [r1, #4]
    mov r0, r4
    sub r4, r4, #1
    b List.LinkedList.get_P_3.L3
List.LinkedList.get_P_3.L4: 
    lsl r1, r5, #2
    add r1, r1, #8
    ldr r0, [r1, #8]
    ldr r2, [r1, #4]
    stmfd sp!, {r0, r2}
    ldr r0, [r1]
    push { r0 }
List.LinkedList.get_P_3.L7: 
    mov r2, sp
    mov sp, fp
    ldmfd sp!, {r3-r5, fp}
    mov r0, #12
    add r1, r2, #12
    add r10, pc, #8
    b _routine_stack_copy_
    mov r10, #0
    bx lr
.P1974009528_NULL: .word NULL

.global List.LinkedList.create_P_3
List.LinkedList.create_P_3: 
    push { fp, lr }
    mov fp, sp
/* Initialize list, set pointers to null */
    add r0, fp, #16
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    mov r0, #0
    ldr r1, .P1974009527_NULL
    ldr r2, .P1974009527_NULL
    stmfd sp!, {r0-r2}
    ldr r0, .P1974009527_List.LinkedList
    push { r0 }
    bl init_P_7
    mov sp, fp
    ldmfd sp!, {fp, lr}
    add sp, sp, #12
    bx lr
.P1974009527_List.LinkedList: .word List.LinkedList
.P1974009527_NULL: .word NULL

.global List.LinkedList.add_P_4
List.LinkedList.add_P_4: 
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3, r4, fp, lr}
    mov fp, sp
    mov r3, r0
/* Initialize the new List Node, set the next pointer to null */
    add r0, fp, #28
    ldmfa r0, {r0-r2}
    stmfd sp!, {r0-r2}
    ldr r0, [fp, #16]
    push { r0 }
    ldr r0, .P1974009529_NULL
    push { r0 }
    ldr r0, .P1974009529_List.ListNode
    push { r0 }
    bl init_P_6
    mov r4, r0
    lsl r1, r3, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009529_NULL
    cmp r0, r1
    bne List.LinkedList.add_P_4.L1
    lsl r1, r3, #2
    str r4, [r1, #4]
    mov r0, r4
    lsl r1, r3, #2
    str r4, [r1, #8]
    b List.LinkedList.add_P_4.L0
List.LinkedList.add_P_4.L1: 
    lsl r1, r3, #2
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    str r4, [r1, #4]
    lsl r1, r3, #2
    str r4, [r1, #8]
List.LinkedList.add_P_4.L0: 
    lsl r1, r3, #2
    add r1, r1, #12
    ldr r0, [r1]
    add r2, r0, #1
    str r2, [r1]
    mov sp, fp
    ldmfd sp!, {r3, r4, fp, lr}
    add sp, sp, #16
    bx lr
.P1974009529_List.ListNode: .word List.ListNode
.P1974009529_NULL: .word NULL

.global List.LinkedList.remove_P_1
List.LinkedList.remove_P_1: 
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3-r5, lr}
    mov r3, r0
    mov r4, r1
/* Out of bounds */
    cmp r1, #0
    bge List.LinkedList.remove_P_1.L0
    ldmfd sp!, {r3-r5, pc}
List.LinkedList.remove_P_1.L0: 
    cmp r4, #0
    bne List.LinkedList.remove_P_1.L3
/* Remove first element */
    lsl r1, r3, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009528_NULL
    cmp r0, r1
    ldmfdeq sp!, {r3-r5, pc}
    lsl r1, r3, #2
    ldr r5, [r1, #4]
    lsl r1, r3, #2
    ldr r1, [r1, #4]
    lsl r1, r1, #2
    ldr r0, [r1, #4]
    lsl r1, r3, #2
    str r0, [r1, #4]
/* Only one element in list */
    lsl r1, r3, #2
    ldr r0, [r1, #8]
    cmp r0, r5
    bne List.LinkedList.remove_P_1.L6
    ldr r0, .P1974009528_NULL
    lsl r1, r3, #2
    str r0, [r1, #8]
List.LinkedList.remove_P_1.L6: 
    mov r0, r5
    bl free
    ldmfd sp!, {r3-r5, pc}
List.LinkedList.remove_P_1.L3: 
    lsl r1, r3, #2
    ldr r5, [r1, #4]
    mov r0, r4
    sub r4, r4, #1
/* Jump to element before element to remove */
List.LinkedList.remove_P_1.L10: 
    cmp r4, #0
    ble List.LinkedList.remove_P_1.L11
    lsl r1, r5, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009528_NULL
    cmp r0, r1
    bne List.LinkedList.remove_P_1.L13
    ldmfd sp!, {r3-r5, pc}
List.LinkedList.remove_P_1.L13: 
    lsl r1, r5, #2
    ldr r5, [r1, #4]
    mov r0, r4
    sub r4, r4, #1
    b List.LinkedList.remove_P_1.L10
List.LinkedList.remove_P_1.L11: 
/* Out of bounds */
    lsl r1, r5, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009528_NULL
    cmp r0, r1
    bne List.LinkedList.remove_P_1.L16
    ldmfd sp!, {r3-r5, pc}
List.LinkedList.remove_P_1.L16: 
    lsl r1, r5, #2
    ldr r1, [r1, #4]
    lsl r1, r1, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009528_NULL
    cmp r0, r1
    bne List.LinkedList.remove_P_1.L19
/* Element to remove is last element in list */
    lsl r1, r5, #2
    ldr r0, [r1, #4]
    bl free
    ldr r0, .P1974009528_NULL
    lsl r1, r5, #2
    str r0, [r1, #4]
    mov r0, r5
    lsl r1, r3, #2
    str r5, [r1, #8]
    ldmfd sp!, {r3-r5, pc}
List.LinkedList.remove_P_1.L19: 
/* Cut out element and set next pointers */
    lsl r1, r5, #2
    ldr r1, [r1, #4]
    lsl r1, r1, #2
    ldr r3, [r1, #4]
    lsl r1, r5, #2
    ldr r0, [r1, #4]
    bl free
    mov r0, r3
    lsl r1, r5, #2
    str r3, [r1, #4]
    ldmfd sp!, {r3-r5, pc}
.P1974009528_NULL: .word NULL

.global List.LinkedList.size_P_1
List.LinkedList.size_P_1: 
    mov r10, #0
    lsl r1, r0, #2
    ldr r0, [r1, #12]
    bx lr

.global List.LinkedList.set_P_1
List.LinkedList.set_P_1: 
    mov r10, #0
    sub sp, sp, #12
    stmea sp, {r4-r6}
    mov r4, r1
    mov r5, r2
    lsl r1, r0, #2
    ldr r6, [r1, #4]
List.LinkedList.set_P_1.L1: 
    cmp r4, #0
    beq List.LinkedList.set_P_1.L2
    lsl r1, r6, #2
    ldr r0, [r1, #4]
    ldr r1, .P1974009527_NULL
    cmp r0, r1
    bne List.LinkedList.set_P_1.L3
    b List.LinkedList.set_P_1.L5
List.LinkedList.set_P_1.L3: 
/* Jump to next */
    lsl r1, r6, #2
    ldr r6, [r1, #4]
    mov r0, r4
    sub r4, r4, #1
    b List.LinkedList.set_P_1.L1
List.LinkedList.set_P_1.L2: 
    mov r0, r5
    lsl r1, r6, #2
    str r5, [r1, #8]
List.LinkedList.set_P_1.L5: 
    ldmfd sp!, {r4-r6}
    bx lr
.P1974009527_NULL: .word NULL

.global List.LinkedList.destroy_P_1
List.LinkedList.destroy_P_1: 
    sub sp, sp, #12
    stmea sp, {r3, r4, lr}
    mov r3, r0
    lsl r1, r0, #2
    ldr r4, [r1, #4]
List.LinkedList.destroy_P_1.L1: 
    ldr r1, .P1974009527_NULL
    cmp r4, r1
    beq List.LinkedList.destroy_P_1.L2
    mov r0, r4
    bl free
    lsl r1, r4, #2
    ldr r4, [r1, #4]
    b List.LinkedList.destroy_P_1.L1
List.LinkedList.destroy_P_1.L2: 
    mov r0, r3
    bl free
    ldmfd sp!, {r3, r4, pc}
.P1974009527_NULL: .word NULL

