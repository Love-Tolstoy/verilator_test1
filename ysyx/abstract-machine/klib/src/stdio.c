#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

#define MAX_STR_LEN 2048

typedef struct {
  char flags;
  uint8_t width;
  char specifier;
} fmt_label;

// static int fmtl2str(char *out, const char *fmtl, va_list *ap) {
//   // char *fmtl = **fmt;
//   int fmt_len = 0;
//   // int out_len = 0;
//   // char buf[65];
//   // memset(buf, 0, 65);
//   char *s;
//   int d;
//   int d_len;
//   int d_var;

//   fmt_label l;
//   l.flags = ' ';
//   l.width = 0;
//   if (*fmtl == '0') {
//     l.flags = '0';
//     fmtl ++;
//     fmt_len ++;
//   }
//   l.width = atoi(fmtl);
//   for (int i = 0; i < 2; i ++) {      // max width 99
//     if (*fmtl >= '0' && *fmtl < '9') {
//       fmtl ++;
//       fmt_len ++;
//     }
//   }
//   switch (*fmtl) {
//   case 's':
//     s = va_arg(*ap, char *);
//     int sl = strlen(s);
//     strncpy(out, s, sl);
//     out += sl;
//     break;
//   case 'd':
//     d = va_arg(*ap, int);
//     // char *d_buf = buf;

//     // int neg = 0;
//     if (d < 0) {
//       *out = '-';
//       out++;
//       d = -d;
//     }

//     d_len = 1;
//     d_var = d/10;
//     while (d_var != 0) { d_var /= 10; d_len ++; }
    
//     for (int i = 0; i < (l.width - d_len); i ++) {
//       *out = l.flags;
//       out ++;
//     }

//     for (int i = 0 ;i < d_len; i++) {
//       out[d_len - 1 - i] = d%10+'0';
//       d /= 10;
//     }
//     out += d_len;
//     break;
//   case 'u':case 'x':case 'l':
//     d = va_arg(*ap, uint32_t);
//     d_len = 1;
//     d_var = d/10;
//     while (d_var != 0) { d_var /= 10; d_len ++; }
    
//     for (int i = 0; i < (l.width - d_len); i ++) {
//       *out = l.flags;
//       out ++;
//     }

//     for (int i = 0 ;i < d_len; i++) {
//       out[d_len - 1 - i] = d%10+'0';
//       d /= 10;
//     }
//     out += d_len;
//     break;
//   default:
//     assert(0);
//     break;
//   }
//   *out = '\0';
//   fmtl ++;
//   fmt_len ++;
//   return fmt_len;
// }

int printf(const char *fmt, ...) {
  char out[MAX_STR_LEN];
  
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(out, fmt, ap);
  for (int i = 0; i < MAX_STR_LEN && out[i] != '\0'; i ++) {
    putch(out[i]);
  }
  va_end(ap);
  return ret;
}

// use ptr change str, num >= 0
int int2str(const uint64_t num, char *const ptr) {
  if (num == 0) { return 0; }
  int len = int2str(num/10, ptr);
  *(ptr+len) = num%10 + '0';
  return len+1;
}

// not include 0x
int int2hexstr(const uint64_t num, char *const ptr) {
  char hex[16] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  if (num == 0) { return 0; }
  int len = int2hexstr(num/16, ptr);
  *(ptr+len) = hex[num%16];
  return len+1;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int i = MAX_STR_LEN;
  // char buf[65];
  char *s;
  int64_t d;
  int d_len;
  int64_t d_var;
  uint64_t d64 = 0;
  fmt_label l;

  while (*fmt != '\0' && i --) {
    if (*fmt == '%') {
      fmt++;
      l.flags = ' ';
      l.width = 0;
      if (*fmt == '0') {
        l.flags = '0';
        fmt ++;
      }
      l.width = atoi(fmt);
      for (int i = 0; i < 2; i ++) {      // max width 99
        if (*fmt >= '0' && *fmt < '9') {
          fmt ++;
        }
      }
      switch (*fmt) {
      case 's':
        s = va_arg(ap, char *);
        int sl = strlen(s);
        strncpy(out, s, sl);
        out += sl;
        break;
      case 'd':
        d = va_arg(ap, int64_t);

        if (d < 0) {
          *out = '-';
          out++;
          d = -d;
        }

        d_len = 1;
        d_var = d/10;
        while (d_var != 0) { d_var /= 10; d_len ++; }
        
        for (int i = 0; i < (l.width - d_len); i ++) {
          *out = l.flags;
          out ++;
        }

        // for (int i = 0 ;i < d_len; i++) {
        //   out[d_len - 1 - i] = d%10+'0';
        //   d /= 10;
        // }
        out += int2str(d, out);
        break;
      case 'u':case 'l':
        d = va_arg(ap, uint32_t);
        d_len = 1;
        d_var = d/10;
        while (d_var != 0) { d_var /= 10; d_len ++; }
        
        for (int i = 0; i < (l.width - d_len); i ++) {
          *out = l.flags;
          out ++;
        }
        out += int2str(d, out);
        break;
      case 'x':
        d = va_arg(ap, uint64_t);
        d_len = 1;
        d_var = d/16;
        while (d_var != 0) { d_var /= 16; d_len ++; }
        
        for (int i = 0; i < (l.width - d_len); i ++) {
          *out = l.flags;
          out ++;
        }
        out += int2hexstr(d, out);
        break;
      case 'p':
        d64 = (uint64_t)(intptr_t)va_arg(ap, void *);
        out += int2hexstr(d64, out);
        break;
      default:
        assert(0);
        break;
      }
      // *out = '\0';
      fmt ++;
    } else {
      *out = *fmt;
      // strncpy(out, fmt, 1);
      out ++;
      fmt ++;
    }
  }
  *out = *fmt;
  // strncpy(out, fmt, 1);
  return 0;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(out, fmt, ap);
  va_end(ap);
  return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
