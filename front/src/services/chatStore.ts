import { defineStore } from 'pinia'
import type { ChatErrorResponse, ChatHistoryResponse, ChatMessage } from 'boardgame-web-common/back'

export const LOBBY_ROOM_ID = 'lobby'

export function gameRoomId(gameId: string): string {
    return `game_${gameId}`
}

export interface ChatRoomView {
    messages: ChatMessage[]
    hasMore: boolean
    loaded: boolean
    loading: boolean
    error?: string
}

interface PendingMessage {
    roomId: string
    clientMessageId: string
}

interface ChatState {
    open: boolean
    activeRoomId: string
    rooms: Record<string, ChatRoomView>
    lastReadSeq: Record<string, number>
    pending: Record<string, PendingMessage>
}

function emptyRoom(): ChatRoomView {
    return {
        messages: [],
        hasMore: false,
        loaded: false,
        loading: false
    }
}

export const useChatStore = defineStore('chatStore', {
    state: (): ChatState => ({
        open: false,
        activeRoomId: LOBBY_ROOM_ID,
        rooms: {},
        lastReadSeq: {},
        pending: {}
    }),

    persist: {
        pick: ['lastReadSeq']
    },

    getters: {
        room: (state) => (roomId: string): ChatRoomView => state.rooms[roomId] ?? emptyRoom(),

        unreadCount: (state) => (roomId: string): number => {
            const room = state.rooms[roomId]
            if (!room) return 0
            const lastRead = state.lastReadSeq[roomId] ?? 0
            return room.messages.filter(message => message.seq > lastRead && !message.deleted).length
        },

        totalUnread(): number {
            return Object.keys(this.rooms).reduce((sum, roomId) => sum + this.unreadCount(roomId), 0)
        }
    },

    actions: {
        ensureRoom(roomId: string): ChatRoomView {
            if (!this.rooms[roomId]) {
                this.rooms[roomId] = emptyRoom()
            }
            return this.rooms[roomId]!
        },

        openRoom(roomId: string) {
            this.open = true
            this.activeRoomId = roomId
            this.ensureRoom(roomId)
            this.markRead(roomId)
        },

        close() {
            this.open = false
        },

        markRead(roomId: string) {
            const room = this.rooms[roomId]
            if (!room || room.messages.length === 0) return
            const lastSeq = Math.max(...room.messages.map(message => message.seq))
            this.lastReadSeq[roomId] = Math.max(this.lastReadSeq[roomId] ?? 0, lastSeq)
        },

        addOptimistic(roomId: string, message: ChatMessage, requestId: string) {
            const room = this.ensureRoom(roomId)
            room.messages.push(message)
            room.error = undefined
            if (message.clientMessageId) {
                this.pending[requestId] = { roomId, clientMessageId: message.clientMessageId }
            }
        },

        receiveMessage(message: ChatMessage) {
            const room = this.ensureRoom(message.roomId)

            if (message.clientMessageId) {
                const index = room.messages.findIndex(
                    existing => existing.clientMessageId === message.clientMessageId
                )
                if (index >= 0) {
                    room.messages.splice(index, 1, message)
                } else if (!room.messages.some(existing => existing.id === message.id)) {
                    room.messages.push(message)
                }
            } else if (!room.messages.some(existing => existing.id === message.id)) {
                room.messages.push(message)
            }

            room.messages.sort((left, right) => left.seq - right.seq)
            room.error = undefined

            for (const [requestId, pending] of Object.entries(this.pending)) {
                if (pending.clientMessageId === message.clientMessageId) {
                    delete this.pending[requestId]
                }
            }

            if (this.open && this.activeRoomId === message.roomId) {
                this.markRead(message.roomId)
            }
        },

        receiveHistory(response: ChatHistoryResponse) {
            const room = this.ensureRoom(response.roomId)
            const knownIds = new Set(room.messages.map(message => message.id))
            const older = response.messages.filter(message => !knownIds.has(message.id))
            room.messages = [...older, ...room.messages].sort((left, right) => left.seq - right.seq)
            room.hasMore = response.hasMore
            room.loaded = true
            room.loading = false
            room.error = undefined

            if (this.open && this.activeRoomId === response.roomId) {
                this.markRead(response.roomId)
            }
        },

        receiveError(error: ChatErrorResponse) {
            if (error.id) {
                const pending = this.pending[error.id]
                if (pending) {
                    const room = this.ensureRoom(pending.roomId)
                    const index = room.messages.findIndex(
                        message => message.clientMessageId === pending.clientMessageId
                    )
                    if (index >= 0) room.messages.splice(index, 1)
                    delete this.pending[error.id]
                }
            }

            const room = this.ensureRoom(this.activeRoomId)
            room.loading = false
            room.error = error.message
        },

        setLoading(roomId: string, loading: boolean) {
            this.ensureRoom(roomId).loading = loading
        }
    }
})
