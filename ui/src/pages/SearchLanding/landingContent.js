/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

import DOMPurify from 'dompurify';

export const hasText = value => typeof value === 'string' && value.trim().length > 0;

export const textOf = value => (hasText(value) ? value.trim() : '');

const hasControlChars = value => {
  for (let index = 0; index < value.length; index += 1) {
    const code = value.charCodeAt(index);
    if (code < 32 || code === 127) {
      return true;
    }
  }
  return false;
};

export const safeUrl = url => {
  if (!hasText(url)) {
    return null;
  }
  const value = url.trim();
  if (hasControlChars(value)) {
    return null;
  }
  if (value.startsWith('/') && !value.startsWith('//')) {
    return value;
  }
  try {
    const parsed = new URL(value);
    if (parsed.protocol === 'http:' || parsed.protocol === 'https:' || parsed.protocol === 'mailto:') {
      return value;
    }
  } catch {
    return null;
  }
  return null;
};

export const safeImageSource = value => {
  if (!hasText(value)) {
    return null;
  }
  const source = value.trim();
  if (hasControlChars(source)) {
    return null;
  }
  if (source.startsWith('/') && !source.startsWith('//')) {
    return source;
  }
  if (/^data:image\/[a-zA-Z0-9.+-]+;base64,/i.test(source)) {
    return source;
  }
  try {
    const parsed = new URL(source);
    if (parsed.protocol === 'http:' || parsed.protocol === 'https:') {
      return source;
    }
  } catch {
    return null;
  }
  return null;
};

export const isImageIcon = value => {
  if (!hasText(value)) {
    return false;
  }
  const source = value.trim();
  return source.startsWith('/')
    || source.startsWith('data:image/')
    || /^https?:\/\//i.test(source);
};

export const hasCallToAction = cta => hasText(cta?.label) && !!safeUrl(cta?.url);

const INLINE_SVG = /^\s*(?:<\?xml[\s\S]*?\?>)?\s*(?:<!--[\s\S]*?-->\s*)*<svg[\s>/]/i;

const sanitizeInlineSvg = markup => {
  const svg = DOMPurify.sanitize(markup, {
    USE_PROFILES: { svg: true, svgFilters: true },
    ADD_TAGS: ['style', 'animate', 'animateTransform', 'animateMotion', 'set'],
    FORBID_TAGS: ['script', 'foreignObject', 'iframe', 'embed', 'object']
  });
  return /<svg[\s>/]/i.test(svg) ? svg : null;
};

const isSvgUrl = url => {
  if (url.startsWith('data:image/svg+xml')) {
    return true;
  }
  return url.split(/[?#]/)[0].toLowerCase().endsWith('.svg');
};

export const heroBackground = value => {
  if (!hasText(value) || value.includes('\u0000')) {
    return null;
  }
  const source = value.trim();
  if (INLINE_SVG.test(source)) {
    const svg = sanitizeInlineSvg(source);
    return svg ? { kind: 'inline', svg } : null;
  }
  const url = safeImageSource(source);
  if (!url) {
    return null;
  }
  return { kind: isSvgUrl(url) ? 'svg' : 'image', url };
};
