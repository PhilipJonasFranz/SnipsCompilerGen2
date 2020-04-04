__resv:
	add r0, r0, #4
	push {r1, r2, r3}
	ldr r1, a_heap_start // Depends on heap start
	ldr r1, [r1]
__resv_loop:
	ldr r2, [r1]
	cmp r2, #0
	bne __resv_not_zero
	str r0, [r1]
	mov r0, r1
	pop {r1, r2, r3}
	bx lr
__resv_not_zero:
	bge __resv_not_negative
	rsb r3, r0, #0
	cmp r3, r0
	blt __resv_not_zero_low
	add r3, r2, r0
	str r0, [r1]
	cmp r3, #0
	bne __resv_not_zero_rest
	mov r0, r1
	pop {r1, r2, r3}
	bx lr
__resv_not_zero_rest:
	add r2, r1, r0
	str r3, [r2]
	mov r0, r1
	pop {r1, r2, r3}
	bx lr
__resv_not_zero_low:
	add r1, r1, r3
	b __resv_loop
__resv_not_negative:
	add r1, r1, r2
	b __resv_loop